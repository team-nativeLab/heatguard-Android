package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorkerProfilePhotoUrlCacheTest {
    @Test
    fun `cache accepts URL only when expiry is beyond safety margin`() {
        val cache = WorkerProfilePhotoUrlCache()
        val now = Instant.parse("2026-11-05T00:00:00Z")
        val safePhoto = profilePhoto(expiry = now.plusSeconds(61))
        val boundaryPhoto = profilePhoto(expiry = now.plusSeconds(60))

        assertTrue(cache.storeIfCurrentRevision(safePhoto, SESSION, 0L, now))
        assertEquals(safePhoto, cache.get(SESSION, now))
        assertFalse(cache.storeIfCurrentRevision(boundaryPhoto, SESSION, 0L, now))
    }

    @Test
    fun `malformed or absent photo details are not cached`() {
        val cache = WorkerProfilePhotoUrlCache()
        val now = Instant.parse("2026-11-05T00:00:00Z")

        assertFalse(cache.storeIfCurrentRevision(null, SESSION, 0L, now))
        assertFalse(
            cache.storeIfCurrentRevision(
                ProfilePhoto("https://images.example.test/profile", null),
                SESSION,
                0L,
                now,
            ),
        )
        assertFalse(cache.storeIfCurrentRevision(ProfilePhoto(" ", "invalid"), SESSION, 0L, now))
        assertFalse(
            cache.storeIfCurrentRevision(ProfilePhoto("http://images.example/profile", "invalid"), SESSION, 0L, now),
        )
        assertNull(cache.get(SESSION, now))
    }

    @Test
    fun `expired cache entry is removed on read`() {
        val cache = WorkerProfilePhotoUrlCache()
        val now = Instant.parse("2026-11-05T00:00:00Z")
        val photo = profilePhoto(expiry = now.plusSeconds(120))

        assertTrue(cache.storeIfCurrentRevision(photo, SESSION, 0L, now))
        assertNull(cache.get(SESSION, now.plusSeconds(61)))
        assertNull(cache.get(SESSION, now))
    }

    @Test
    fun `cached lookup skips remote fetch`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val photo = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))
            var fetchCount = 0
            cache.storeIfCurrentRevision(photo, SESSION, 0L)

            val result =
                cache.getOrFetch(
                    sessionGeneration = SESSION,
                    isSessionCurrent = { true },
                ) {
                    fetchCount += 1
                    ApiResult.Success(null)
                }

            assertEquals(ApiResult.Success(photo), result)
            assertEquals(0, fetchCount)
        }

    @Test
    fun `confirmed photo change refreshes and deletion invalidates cached URL`() {
        val cache = WorkerProfilePhotoUrlCache()
        val oldPhoto = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))
        val newPhoto =
            ProfilePhoto(
                photoUrl = "https://images.example.test/replacement?signature=private",
                expiresAt = Instant.now().plus(2, ChronoUnit.HOURS).toString(),
            )
        cache.updateAfterPhotoChange(SESSION, oldPhoto) { true }
        assertEquals(oldPhoto, cache.get(SESSION))

        cache.updateAfterPhotoChange(SESSION, newPhoto) { true }

        assertEquals(newPhoto, cache.get(SESSION))

        cache.invalidate(SESSION)

        assertNull(cache.get(SESSION))
    }

    @Test
    fun `late response cannot restore URL after invalidation`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val oldPhoto = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))

            cache.getOrFetch(
                sessionGeneration = SESSION,
                isSessionCurrent = { true },
            ) {
                cache.invalidate(SESSION)
                ApiResult.Success(oldPhoto)
            }

            assertNull(cache.get(SESSION))
        }

    @Test
    fun `session clear removes revision state and rejects stale response`() {
        val cache = WorkerProfilePhotoUrlCache()
        val now = Instant.parse("2026-11-05T00:00:00Z")
        val photo = profilePhoto(expiry = now.plusSeconds(3600))
        cache.invalidate(SESSION)

        cache.clearSession(SESSION)

        assertTrue(
            cache.storeIfCurrentRevision(
                photo = photo,
                sessionGeneration = SESSION,
                expectedRevision = 0L,
                now = now,
            ),
        )
        cache.clearSession(SESSION)
        assertFalse(
            cache.storeIfCurrentRevision(
                photo = photo,
                sessionGeneration = SESSION,
                expectedRevision = 0L,
                now = now,
                isSessionCurrent = { false },
            ),
        )
        assertNull(cache.get(SESSION, now))
    }

    @Test
    fun `invalidation detaches old flight and opens a new flight`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val oldFetchStarted = CompletableDeferred<Unit>()
            val releaseOldFetch = CompletableDeferred<Unit>()
            val oldPhoto = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))
            val newPhoto =
                ProfilePhoto(
                    photoUrl = "https://images.example.test/replacement?signature=new",
                    expiresAt = Instant.now().plus(2, ChronoUnit.HOURS).toString(),
                )
            val oldFetch =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        oldFetchStarted.complete(Unit)
                        releaseOldFetch.await()
                        ApiResult.Success(oldPhoto)
                    }
                }
            oldFetchStarted.await()

            cache.invalidate(SESSION)
            var newFetchCount = 0
            val newResult =
                cache.getOrFetch(SESSION, { true }) {
                    newFetchCount += 1
                    ApiResult.Success(newPhoto)
                }
            releaseOldFetch.complete(Unit)

            assertEquals(ApiResult.Success(newPhoto), newResult)
            assertEquals(ApiResult.Success(oldPhoto), oldFetch.await())
            assertEquals(1, newFetchCount)
            assertEquals(newPhoto, cache.get(SESSION))
        }

    @Test
    fun `concurrent cache misses share one successful fetch`() =
        runBlocking {
            val photo = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))

            assertConcurrentLookupsShareResult(ApiResult.Success(photo), expectCached = true)
        }

    @Test
    fun `concurrent null result is shared but not cached`() =
        runBlocking {
            assertConcurrentLookupsShareResult(ApiResult.Success(null), expectCached = false)
        }

    @Test
    fun `concurrent failure result is shared but not cached`() =
        runBlocking {
            assertConcurrentLookupsShareResult(ApiResult.Failure(ApiError.Network), expectCached = false)
        }

    @Test
    fun `concurrent near expiry result is shared but not cached`() =
        runBlocking {
            val photo = profilePhoto(expiry = Instant.now().plusSeconds(30))

            assertConcurrentLookupsShareResult(ApiResult.Success(photo), expectCached = false)
        }

    @Test
    fun `concurrent fetch exception is shared and rethrown`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val fetchStarted = CompletableDeferred<Unit>()
            val releaseFetch = CompletableDeferred<Unit>()
            val expectedException = IllegalStateException("remote fetch failed")
            var fetchCount = 0
            val fetch: suspend () -> ApiResult<ProfilePhoto?> = {
                fetchCount += 1
                fetchStarted.complete(Unit)
                releaseFetch.await()
                throw expectedException
            }

            val first =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        cache.getOrFetch(SESSION, { true }, fetch)
                    }
                }
            fetchStarted.await()
            val second =
                async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching {
                        cache.getOrFetch(SESSION, { true }, fetch)
                    }
                }
            releaseFetch.complete(Unit)

            assertEquals(expectedException, first.await().exceptionOrNull())
            assertEquals(expectedException, second.await().exceptionOrNull())
            assertEquals(1, fetchCount)
        }

    @Test
    fun `cancelled flight owner does not cancel waiter and waiter retries`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val fetchStarted = CompletableDeferred<Unit>()
            val expectedCancellation = CancellationException("flight owner cancelled")
            var waiterFetchCount = 0
            val photo = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))
            val owner =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        fetchStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            fetchStarted.await()
            val waiter =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        waiterFetchCount += 1
                        ApiResult.Success(photo)
                    }
                }

            owner.cancel(expectedCancellation)
            owner.join()

            assertEquals(ApiResult.Success(photo), waiter.await())
            assertEquals(1, waiterFetchCount)
            assertEquals(photo, cache.get(SESSION))
        }

    @Test
    fun `cancelled waiter does not cancel active owner fetch`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val fetchStarted = CompletableDeferred<Unit>()
            val releaseFetch = CompletableDeferred<Unit>()
            val expectedCancellation = CancellationException("waiter cancelled")
            var fetchCount = 0
            val photo = profilePhoto(expiry = Instant.now().plus(1, ChronoUnit.HOURS))
            val owner =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        fetchCount += 1
                        fetchStarted.complete(Unit)
                        releaseFetch.await()
                        ApiResult.Success(photo)
                    }
                }
            fetchStarted.await()
            val waiter =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        fetchCount += 1
                        ApiResult.Success(null)
                    }
                }

            waiter.cancel(expectedCancellation)
            waiter.join()
            releaseFetch.complete(Unit)

            assertTrue(waiter.isCancelled)
            assertEquals(ApiResult.Success(photo), owner.await())
            assertEquals(1, fetchCount)
        }

    @Test
    fun `owner cancellation is rethrown to owner`() =
        runBlocking {
            val cache = WorkerProfilePhotoUrlCache()
            val fetchStarted = CompletableDeferred<Unit>()
            val expectedCancellation = CancellationException("owner cancelled")
            val owner =
                async(start = CoroutineStart.UNDISPATCHED) {
                    cache.getOrFetch(SESSION, { true }) {
                        fetchStarted.complete(Unit)
                        awaitCancellation()
                    }
                }

            fetchStarted.await()
            owner.cancel(expectedCancellation)
            owner.join()

            assertTrue(owner.isCancelled)
        }

    private suspend fun CoroutineScope.assertConcurrentLookupsShareResult(
        expectedResult: ApiResult<ProfilePhoto?>,
        expectCached: Boolean,
    ) {
        val cache = WorkerProfilePhotoUrlCache()
        val fetchStarted = CompletableDeferred<Unit>()
        val releaseFetch = CompletableDeferred<Unit>()
        var fetchCount = 0
        val fetch: suspend () -> ApiResult<ProfilePhoto?> = {
            fetchCount += 1
            fetchStarted.complete(Unit)
            releaseFetch.await()
            expectedResult
        }
        val first =
            async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrFetch(SESSION, { true }, fetch)
            }
        fetchStarted.await()
        val second =
            async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrFetch(SESSION, { true }, fetch)
            }
        releaseFetch.complete(Unit)

        assertEquals(expectedResult, first.await())
        assertEquals(expectedResult, second.await())
        assertEquals(1, fetchCount)

        cache.getOrFetch(SESSION, { true }) {
            fetchCount += 1
            expectedResult
        }
        assertEquals(if (expectCached) 1 else 2, fetchCount)
    }

    private fun profilePhoto(expiry: Instant): ProfilePhoto =
        ProfilePhoto(
            photoUrl = "https://images.example.test/photo?signature=private",
            expiresAt = expiry.toString(),
        )

    private companion object {
        const val SESSION = 42L
    }
}
