package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerProfilePhotoUrlCache
    @Inject
    constructor() {
        private val stateLock = Any()
        private val entriesBySession = mutableMapOf<Long, CacheEntry>()
        private val revisionsBySession = mutableMapOf<Long, Long>()
        private val inFlightBySession = mutableMapOf<Long, InFlightPhotoLookup>()

        internal suspend fun getOrFetch(
            sessionGeneration: Long,
            isSessionCurrent: () -> Boolean,
            fetch: suspend () -> ApiResult<ProfilePhoto?>,
        ): ApiResult<ProfilePhoto?> {
            val selection =
                synchronized(stateLock) {
                    getLocked(sessionGeneration, Instant.now())?.let { cachedPhoto ->
                        return ApiResult.Success(cachedPhoto)
                    }
                    val currentFlight = inFlightBySession[sessionGeneration]
                    if (currentFlight != null) {
                        LookupSelection.Follow(currentFlight)
                    } else {
                        val newFlight = InFlightPhotoLookup(revisionFor(sessionGeneration))
                        inFlightBySession[sessionGeneration] = newFlight
                        LookupSelection.Fetch(newFlight)
                    }
                }

            return when (selection) {
                is LookupSelection.Follow -> {
                    when (val outcome = selection.flight.awaitOutcome()) {
                        is FlightOutcome.Completed -> outcome.apiResult
                        is FlightOutcome.Failed -> throw outcome.exception
                        FlightOutcome.RetryAfterOwnerCancellation ->
                            getOrFetch(sessionGeneration, isSessionCurrent, fetch)
                    }
                }
                is LookupSelection.Fetch -> fetchAndShare(
                    sessionGeneration = sessionGeneration,
                    isSessionCurrent = isSessionCurrent,
                    fetch = fetch,
                    flight = selection.flight,
                )
            }
        }

        private suspend fun fetchAndShare(
            sessionGeneration: Long,
            isSessionCurrent: () -> Boolean,
            fetch: suspend () -> ApiResult<ProfilePhoto?>,
            flight: InFlightPhotoLookup,
        ): ApiResult<ProfilePhoto?> {
            try {
                val result = fetch()
                if (result is ApiResult.Success) {
                    storeIfCurrentRevision(
                        photo = result.value,
                        sessionGeneration = sessionGeneration,
                        expectedRevision = flight.revision,
                        now = Instant.now(),
                        isSessionCurrent = isSessionCurrent,
                    )
                }
                flight.result.complete(FlightOutcome.Completed(result))
                return result
            } catch (cancellationException: CancellationException) {
                synchronized(stateLock) {
                    if (inFlightBySession[sessionGeneration] === flight) {
                        inFlightBySession.remove(sessionGeneration)
                    }
                }
                flight.result.complete(FlightOutcome.RetryAfterOwnerCancellation)
                throw cancellationException
            } catch (exception: Throwable) {
                flight.result.complete(FlightOutcome.Failed(exception))
                throw exception
            } finally {
                synchronized(stateLock) {
                    if (inFlightBySession[sessionGeneration] === flight) {
                        inFlightBySession.remove(sessionGeneration)
                    }
                }
            }
        }

        internal fun updateAfterPhotoChange(
            sessionGeneration: Long,
            photo: ProfilePhoto?,
            isSessionCurrent: () -> Boolean,
        ) {
            synchronized(stateLock) {
                removeEntryAndIncrementRevision(sessionGeneration)
                inFlightBySession.remove(sessionGeneration)
                if (isSessionCurrent()) {
                    storeIfUsable(photo, sessionGeneration, Instant.now())
                }
            }
        }

        internal fun clearSession(sessionGeneration: Long) {
            synchronized(stateLock) {
                entriesBySession.remove(sessionGeneration)
                revisionsBySession.remove(sessionGeneration)
                inFlightBySession.remove(sessionGeneration)
            }
        }

        internal fun get(
            sessionGeneration: Long,
            now: Instant = Instant.now(),
        ): ProfilePhoto? =
            synchronized(stateLock) {
                val entry = entriesBySession[sessionGeneration] ?: return@synchronized null
                if (!entry.expiresAt.isAfter(now.plusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS))) {
                    entriesBySession.remove(sessionGeneration)
                    return@synchronized null
                }
                entry.photo
            }

        internal fun storeIfCurrentRevision(
            photo: ProfilePhoto?,
            sessionGeneration: Long,
            expectedRevision: Long,
            now: Instant = Instant.now(),
            isSessionCurrent: () -> Boolean = { true },
        ): Boolean =
            synchronized(stateLock) {
                if (revisionFor(sessionGeneration) != expectedRevision || !isSessionCurrent()) {
                    return@synchronized false
                }
                storeIfUsable(photo, sessionGeneration, now)
            }

        internal fun invalidate(sessionGeneration: Long) {
            synchronized(stateLock) {
                removeEntryAndIncrementRevision(sessionGeneration)
                inFlightBySession.remove(sessionGeneration)
            }
        }

        private fun storeIfUsable(
            photo: ProfilePhoto?,
            sessionGeneration: Long,
            now: Instant,
        ): Boolean {
            val validPhoto = photo ?: return false
            val expiry = validPhoto.validExpiryAt(now) ?: return false
            entriesBySession[sessionGeneration] = CacheEntry(validPhoto, expiry)
            return true
        }

        private fun revisionFor(sessionGeneration: Long): Long =
            synchronized(stateLock) {
                revisionsBySession[sessionGeneration] ?: 0L
            }

        private fun getLocked(
            sessionGeneration: Long,
            now: Instant,
        ): ProfilePhoto? {
            val entry = entriesBySession[sessionGeneration] ?: return null
            if (!entry.expiresAt.isAfter(now.plusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS))) {
                entriesBySession.remove(sessionGeneration)
                return null
            }
            return entry.photo
        }

        private fun removeEntryAndIncrementRevision(sessionGeneration: Long) {
            entriesBySession.remove(sessionGeneration)
            revisionsBySession[sessionGeneration] = revisionFor(sessionGeneration) + 1L
        }

        private fun ProfilePhoto.validExpiryAt(now: Instant): Instant? {
            val url = photoUrl?.takeIf(String::isNotBlank) ?: return null
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase(Locale.ROOT) != HTTPS_SCHEME || uri.host.isNullOrBlank()) {
                return null
            }
            val expiry = runCatching { OffsetDateTime.parse(expiresAt).toInstant() }.getOrNull() ?: return null
            if (!expiry.isAfter(now.plusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS))) {
                return null
            }
            return expiry
        }

        private data class CacheEntry(
            val photo: ProfilePhoto,
            val expiresAt: Instant,
        )

        private class InFlightPhotoLookup(
            val revision: Long,
            val result: CompletableDeferred<FlightOutcome> = CompletableDeferred(),
        ) {
            suspend fun awaitOutcome(): FlightOutcome = result.await()
        }

        private sealed interface LookupSelection {
            data class Fetch(val flight: InFlightPhotoLookup) : LookupSelection

            data class Follow(val flight: InFlightPhotoLookup) : LookupSelection
        }

        private sealed interface FlightOutcome {
            data class Completed(val apiResult: ApiResult<ProfilePhoto?>) : FlightOutcome

            data class Failed(val exception: Throwable) : FlightOutcome

            data object RetryAfterOwnerCancellation : FlightOutcome
        }

        private companion object {
            const val HTTPS_SCHEME = "https"
            const val EXPIRY_SAFETY_MARGIN_SECONDS = 60L
        }
    }
