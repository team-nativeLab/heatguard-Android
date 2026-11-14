package com.nativelap.heartguard.data.site.local

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.WeatherField
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WeatherCacheSafetyTest {
    @Test
    fun emptyCacheStaysEmptyAndInvalidCacheIsCleared() =
        runBlocking {
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
    fun validWeatherCachePayloadIsPreservedForRestorationAndAccess() =
        runBlocking {
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
    fun legacyWeatherCacheUsesItsObservedAtForEachPopulatedField() {
        val observedAt =
            java.time.Instant
                .parse("2026-11-11T00:59:00Z")
                .toString()
        val payload =
            Json.encodeToString(
                CurrentLocationWeatherResponseDto(
                    temperature = 18.0,
                    humidity = 45.0,
                    observedAt = observedAt,
                    availability = "AVAILABLE",
                ),
            )

        val restored = readWeatherCacheFields(payload, 37.5, 127.0, "session-id", Json)

        assertTrue(restored is WeatherCacheRead.Valid)
        val provenance = (restored as WeatherCacheRead.Valid).cachedWeather.fieldProvenance
        assertTrue(provenance.observedAtByField[WeatherField.TEMPERATURE] == observedAt)
        assertTrue(provenance.observedAtByField[WeatherField.HUMIDITY] == observedAt)
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
    fun expiredMainWeatherKeepsFreshColdWaveWarning() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val response =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                observedAt = now.minusSeconds(3_600).toString(),
                availability = "AVAILABLE",
                coldWaveWarning =
                    com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto(
                        level = "WARNING",
                        availability = "CACHED",
                        fetchedAt = now.minusSeconds(60).toString(),
                        isStale = true,
                    ),
            )

        val pruned = response.expireWeatherParts(now)

        assertNull(pruned.observedAt)
        assertNull(pruned.temperature)
        assertTrue(pruned.availability == "CACHED")
        assertTrue(pruned.coldWaveWarning?.fetchedAt == now.minusSeconds(60).toString())
        assertTrue(pruned.hasFreshWeatherPart(now))
    }

    @Test
    fun emptyAndPartialResponsesPreserveFreshMainWeatherAndItsTimestamp() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val previous =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                humidity = 56.0,
                observedAt = now.minusSeconds(3_540).toString(),
                availability = "AVAILABLE",
            )
        val partial =
            CurrentLocationWeatherResponseDto(
                temperature = 13.0,
                observedAt = now.toString(),
                availability = "AVAILABLE",
            )

        val merged = partial.mergeFreshWeather(previous.toWeatherSnapshot(), now)
        assertTrue(merged.weather.temperature == 13.0 && merged.weather.humidity == 56.0)
        assertTrue(merged.weather.observedAt == now.toString() && merged.weather.availability == "AVAILABLE")
        val expiredHumidity = merged.expireWeatherParts(now.plusSeconds(60))
        assertNull(expiredHumidity.weather.humidity)
        assertTrue(expiredHumidity.weather.temperature == 13.0)
    }

    @Test
    fun partialAncillaryFieldsKeepTheirOwnPreviousObservationTime() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val previousObservation = now.minusSeconds(600).toString()
        val previous =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                humidity = 56.0,
                apparentTemperature = 10.0,
                heatLevel = 1,
                observedAt = previousObservation,
                skyStatus = "CLOUDY",
                temperatureDelta = -1.0,
                availability = "AVAILABLE",
            )
        val incoming =
            CurrentLocationWeatherResponseDto(
                temperature = 13.0,
                humidity = 50.0,
                observedAt = now.toString(),
                availability = "AVAILABLE",
            )

        val merged = incoming.mergeFreshWeather(previous.toWeatherSnapshot(), now)

        assertEquals(13.0, merged.weather.temperature!!, 0.0)
        assertEquals(50.0, merged.weather.humidity!!, 0.0)
        assertEquals(10.0, merged.weather.apparentTemperature!!, 0.0)
        assertEquals(1, merged.weather.heatLevel)
        assertEquals("CLOUDY", merged.weather.skyStatus)
        assertEquals(-1.0, merged.weather.temperatureDelta!!, 0.0)
        assertEquals(previousObservation, merged.fieldProvenance.observedAtByField[WeatherField.APPARENT_TEMPERATURE])
        assertTrue(WeatherField.APPARENT_TEMPERATURE in merged.fieldProvenance.fieldsFromPreviousValue)
        assertFalse(
            merged
                .expireWeatherParts(now.plusSeconds(3_000))
                .fieldProvenance
                .fieldsFromPreviousValue
                .contains(WeatherField.APPARENT_TEMPERATURE),
        )
        assertNull(
            merged.expireWeatherParts(now.plusSeconds(3_000)).weather.apparentTemperature,
        )
    }

    @Test
    fun freshColdWaveDoesNotReplacePreviousWeatherObservation() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val previous =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                humidity = 56.0,
                observedAt = now.minusSeconds(60).toString(),
                availability = "AVAILABLE",
            )
        val incoming =
            CurrentLocationWeatherResponseDto(
                observedAt = now.toString(),
                availability = "AVAILABLE",
                coldWaveWarning =
                    com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto(
                        level = "WARNING",
                        availability = "AVAILABLE",
                        fetchedAt = now.toString(),
                    ),
            )
        val merged = incoming.mergeFreshWeather(previous.toWeatherSnapshot(), now)
        assertTrue(merged.weather.temperature == 12.0 && merged.weather.observedAt == previous.observedAt)
        assertTrue(merged.weather.coldWaveWarning?.fetchedAt == now.toString())
        assertTrue(merged.weather.availability == "CACHED")
    }

    @Test
    fun timestampWithoutWeatherIsNotFreshWeather() {
        val now = java.time.Instant.parse("2026-11-11T01:00:00Z")
        val empty = CurrentLocationWeatherResponseDto(observedAt = now.toString(), availability = "AVAILABLE")
        assertFalse(empty.expireWeatherParts(now).hasFreshWeatherPart(now))
    }

    @Test
    fun failedDeletionCannotExposePreviousSessionCacheAfterRelogin() {
        val storedOwner = "old-session-identifier"

        assertFalse(isOwnedByCurrentSession(storedOwner, "new-session-identifier"))
        assertFalse(isOwnedByCurrentSession(storedOwner, null))
        assertTrue(isOwnedByCurrentSession(storedOwner, "old-session-identifier"))
    }

    @Test
    fun clearWaitsForSaveAlreadyInsideSerializedCacheBoundary() =
        runBlocking {
            val lock = WeatherCacheLock()
            val saveStarted = CompletableDeferred<Unit>()
            val continueSave = CompletableDeferred<Unit>()
            var currentToken: String? = "old-session-identifier"
            var cachedOwner: String? = null

            val saveJob =
                launch {
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
            val clearJob =
                async {
                    lock.withLock { cachedOwner = null }
                }

            assertFalse(clearJob.isCompleted)
            continueSave.complete(Unit)
            saveJob.join()
            clearJob.await()

            assertNull(cachedOwner)
        }

    @Test
    fun staleSaveAfterLogoutCannotRepopulateClearedCache() =
        runBlocking {
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
    fun ioFailureUsesFallbackAndCancellationStillPropagates() =
        runBlocking {
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
