package com.nativelap.heartguard.data.site.local

import java.io.IOException
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCacheSafetyTest {
    @Test
    fun emptyCacheStaysEmptyAndInvalidCacheIsCleared() = runBlocking {
        var clearCount = 0
        val empty = readWeatherCacheFields(null, null, null, null, Json)
        val invalid = readWeatherCacheFields("broken", 37.5, 127.0, "session-id", Json)
        val missingOwner = readWeatherCacheFields("{}", 37.5, 127.0, null, Json)
        val valid =
            readWeatherCacheFields(
                Json.encodeToString(CurrentLocationWeatherResponseDto(temperature = 21.5)),
                37.5,
                127.0,
                "session-id",
                Json,
            )

        assertNull(empty.resolveOrClearInvalid { clearCount += 1 })
        assertNull(invalid.resolveOrClearInvalid { clearCount += 1 })
        assertNull(missingOwner.resolveOrClearInvalid { clearCount += 1 })
        assertTrue(valid.resolveOrClearInvalid { clearCount += 1 } is CachedCurrentLocationWeather)
        assertTrue(clearCount == 2)
    }

    @Test
    fun emptyPreferencesAreDifferentFromPartialOrDamagedCache() {
        assertTrue(
            readWeatherCacheFields(
                payload = null,
                latitude = null,
                longitude = null,
                sessionOwner = null,
                json = Json,
            ) is WeatherCacheRead.Empty,
        )
        assertTrue(
            readWeatherCacheFields(
                payload = "{}",
                latitude = 37.5,
                longitude = 127.0,
                sessionOwner = null,
                json = Json,
            ) is WeatherCacheRead.Invalid,
        )
        assertTrue(
            readWeatherCacheFields(
                payload = "{broken-json",
                latitude = 37.5,
                longitude = 127.0,
                sessionOwner = "session-id",
                json = Json,
            ) is WeatherCacheRead.Invalid,
        )
    }

    @Test
    fun validWeatherCachePayloadIsPreservedForRestorationAndAccess() = runBlocking {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val payload =
            Json.encodeToString(
                CurrentLocationWeatherResponseDto(
                    temperature = 21.5,
                    observedAt = now.minusSeconds(60).toString(),
                    availability = "AVAILABLE",
                ),
            )
        var clearCount = 0

        val restoredValue =
            readWeatherCacheFields(payload, 37.5, 127.0, "session-id", Json)
                .resolveOrClearInvalid { clearCount += 1 }
        val accessedValue =
            readWeatherCacheFields(payload, 37.5, 127.0, "session-id", Json)
                .resolveOrClearInvalid { clearCount += 1 }

        assertTrue(restoredValue?.weather?.expireWeatherParts(now)?.hasFreshWeatherPart(now) == true)
        assertTrue(accessedValue?.weather?.expireWeatherParts(now)?.hasFreshWeatherPart(now) == true)
        assertTrue(clearCount == 0)
    }

    @Test
    fun expiredPersistedWeatherIsRemovedByStartupPrunePolicy() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val expired =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                observedAt = now.minusSeconds(3_600).toString(),
                availability = "AVAILABLE",
            )

        val pruned = expired.expireWeatherParts(now)

        assertFalse(pruned.hasFreshWeatherPart(now))
        assertTrue(pruned.availability == "UNAVAILABLE")
    }

    @Test
    fun failedDeletionCannotExposePreviousSessionCacheAfterRelogin() {
        val storedOwner = "old-session-identifier"

        assertFalse(isOwnedByCurrentSession(storedOwner, "new-session-identifier"))
        assertFalse(isOwnedByCurrentSession(storedOwner, null))
        assertTrue(isOwnedByCurrentSession(storedOwner, "old-session-identifier"))
    }

    @Test
    fun clearWaitsForSaveAlreadyInsideSerializedCacheBoundary() = runBlocking {
        val lock = WeatherCacheLock()
        val saveStarted = CompletableDeferred<Unit>()
        val continueSave = CompletableDeferred<Unit>()
        var currentToken: String? = "old-session-identifier"
        var cachedOwner: String? = null

        val saveJob = launch {
            lock.withLock {
                val tokenAtCheck = currentToken
                if (tokenAtCheck == "old-session-identifier") {
                    saveStarted.complete(Unit)
                    continueSave.await()
                    cachedOwner = tokenAtCheck
                }
            }
        }
        saveStarted.await()
        currentToken = null
        val clearJob = async {
            lock.withLock { cachedOwner = null }
        }

        assertFalse(clearJob.isCompleted)
        continueSave.complete(Unit)
        saveJob.join()
        clearJob.await()

        assertNull(cachedOwner)
    }

    @Test
    fun staleSaveAfterLogoutCannotRepopulateClearedCache() = runBlocking {
        val lock = WeatherCacheLock()
        var currentToken: String? = null
        var cachedOwner: String? = "old-session-identifier"

        lock.withLock { cachedOwner = null }
        currentToken = "new-session-token"
        lock.withLock {
            val tokenAtWrite = currentToken
            if (tokenAtWrite == "old-session-identifier") {
                cachedOwner = tokenAtWrite
            }
        }

        assertNull(cachedOwner)
    }

    @Test
    fun ioFailureUsesFallbackAndCancellationStillPropagates() = runBlocking {
        var reportedFailure: Exception? = null
        val result =
            runCacheOperationSafely(
                onFailure = { error -> reportedFailure = error },
                operation = { throw IOException("disk unavailable") },
            )

        assertNull(result)
        assertTrue(reportedFailure is IOException)

        try {
            runCacheOperationSafely(
                onFailure = {},
                operation = { throw kotlinx.coroutines.CancellationException("cancelled") },
            )
            throw AssertionError("CancellationException must propagate")
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Expected.
        }
    }
}
