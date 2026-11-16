package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.local.CachedCurrentLocationWeather
import com.nativelap.heartguard.data.site.local.expireWeatherParts
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.model.WeatherField
import com.nativelap.heartguard.domain.site.model.WeatherUnavailableReason
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LiveWeatherResponseTest {
    private val now = Instant.parse("2026-11-11T14:20:00Z")
    private val response =
        CurrentLocationWeatherResponseDto(
            temperature = 8.7,
            humidity = 77.0,
            observedAt = now.minusSeconds(300).toString(),
            availability = "AVAILABLE",
            isStale = false,
        )

    @Test
    fun availableLiveResponseUsesReceiptTimeWhenObservationIsOlderThanCacheLifetime() {
        val oldObservationResponse = response.copy(observedAt = now.minusSeconds(4_800).toString())

        val weather = oldObservationResponse.toDisplayableLiveWeather(now)

        assertEquals(WeatherAvailability.AVAILABLE, weather?.availability)
        assertEquals(oldObservationResponse.observedAt, weather?.observedAt)
        assertEquals(now.toString(), weather?.serverReceivedAt)
    }

    @Test
    fun serverJsonDecodesMainWeatherStaleness() {
        val payload =
            """
            {"temperature":8.7,"observedAt":"2026-11-11T13:00:00Z",
             "availability":"AVAILABLE","isStale":true}
            """.trimIndent()
        val parsed = Json.decodeFromString<CurrentLocationWeatherResponseDto>(payload)
        assertEquals(true, parsed.isStale)
        assertNull(parsed.toDisplayableLiveWeather(now))
    }

    @Test
    fun staleUnavailableAndInvalidObservationsCannotBeLive() {
        assertNull(response.copy(isStale = true).toDisplayableLiveWeather(now))
        assertNull(response.copy(availability = "CACHED").toDisplayableLiveWeather(now))
        assertNull(response.copy(availability = "UNAVAILABLE").toDisplayableLiveWeather(now))
        assertNull(response.copy(observedAt = null).toDisplayableLiveWeather(now))
        assertNull(response.copy(observedAt = "invalid").toDisplayableLiveWeather(now))
        assertNull(response.copy(observedAt = now.plusSeconds(1).toString()).toDisplayableLiveWeather(now))
    }

    @Test
    fun freshCachedMainWinsOverOlderLiveObservationAfterRestart() {
        val previous =
            response.copy(
                temperature = 12.0,
                humidity = 45.0,
                observedAt = now.minusSeconds(300).toString(),
            )
        val olderIncoming = response.copy(observedAt = now.minusSeconds(600).toString())
        val cached =
            CachedCurrentLocationWeather(
                latitude = 37.5,
                longitude = 127.0,
                weather = previous,
                sessionOwner = "session",
            )
        val selected =
            selectWeatherForCache(
                olderIncoming,
                cached,
                37.5,
                127.0,
                now,
            )
        val weather = resolveReturnedWeather(olderIncoming, selected.weather, now)
        assertEquals(12.0, weather.temperature!!, 0.0)
        assertEquals(45.0, weather.humidity!!, 0.0)
        assertEquals(WeatherAvailability.CACHED, weather.availability)
    }

    @Test
    fun freshLiveCoreValuesKeepMissingCachedMetricsWithTheirOriginalTimes() {
        val previousObservation = now.minusSeconds(300).toString()
        val previous =
            response.copy(
                temperature = 12.0,
                humidity = 45.0,
                apparentTemperature = 10.0,
                heatLevel = 1,
                observedAt = previousObservation,
                skyStatus = "CLOUDY",
                temperatureDelta = -1.0,
            )
        val cached =
            CachedCurrentLocationWeather(
                latitude = 37.5,
                longitude = 127.0,
                weather = previous,
                sessionOwner = "session",
            )
        val incoming =
            response.copy(
                temperature = 13.0,
                humidity = 47.0,
                apparentTemperature = null,
                heatLevel = 2,
                observedAt = now.toString(),
                skyStatus = null,
                temperatureDelta = null,
            )

        val selected =
            selectWeatherForCache(
                incoming,
                cached,
                37.5,
                127.0,
                now,
            )
        val weather = resolveReturnedWeather(incoming, selected.weather, now)

        assertEquals(13.0, weather.temperature!!, 0.0)
        assertEquals(47.0, weather.humidity!!, 0.0)
        assertEquals(10.0, weather.apparentTemperature!!, 0.0)
        assertEquals(2, weather.heatLevel)
        assertEquals(SkyStatus.CLOUDY, weather.skyStatus)
        assertEquals(previousObservation, weather.fieldProvenance.observedAtByField[WeatherField.APPARENT_TEMPERATURE])
        assertEquals(previousObservation, weather.fieldProvenance.observedAtByField[WeatherField.SKY_STATUS])
        assertEquals(previousObservation, weather.fieldProvenance.observedAtByField[WeatherField.TEMPERATURE_DELTA])
        assertTrue(WeatherField.APPARENT_TEMPERATURE in weather.fieldProvenance.fieldsFromPreviousValue)
        assertEquals(WeatherAvailability.AVAILABLE, weather.availability)
    }

    @Test
    fun expiredCachedMetricIsNotRestoredWhenLiveResponseFieldIsNull() {
        val previous =
            response.copy(
                apparentTemperature = 10.0,
                observedAt = now.minusSeconds(3_601).toString(),
            )
        val cached =
            CachedCurrentLocationWeather(
                latitude = 37.5,
                longitude = 127.0,
                weather = previous,
                sessionOwner = "session",
            )
        val incoming =
            response.copy(
                temperature = 13.0,
                humidity = 47.0,
                apparentTemperature = null,
                observedAt = now.toString(),
            )
        val selected = selectWeatherForCache(incoming, cached, 37.5, 127.0, now)

        val weather = resolveReturnedWeather(incoming, selected.weather, now)

        assertEquals(13.0, weather.temperature!!, 0.0)
        assertEquals(47.0, weather.humidity!!, 0.0)
        assertNull(weather.apparentTemperature)
        assertNull(weather.fieldProvenance.observedAtByField[WeatherField.APPARENT_TEMPERATURE])
    }

    @Test
    fun freshCachedWarningSurvivesUnavailableWarningWithOlderLiveMain() {
        val warning =
            ColdWaveWarningDto(
                level = "WARNING",
                availability = "AVAILABLE",
                fetchedAt = now.minusSeconds(300).toString(),
            )
        val previous =
            response.copy(
                temperature = null,
                humidity = null,
                observedAt = null,
                coldWaveWarning = warning,
            )
        val cached =
            CachedCurrentLocationWeather(
                latitude = 37.5,
                longitude = 127.0,
                weather = previous,
                sessionOwner = "session",
            )
        val incoming = response.copy(coldWaveWarning = ColdWaveWarningDto(availability = "UNAVAILABLE"))
        val selected =
            selectWeatherForCache(
                incoming,
                cached,
                37.5,
                127.0,
                now,
            )
        val weather = resolveReturnedWeather(incoming, selected.weather, now)
        assertEquals(8.7, weather.temperature!!, 0.0)
        assertEquals(ColdWaveLevel.WARNING, weather.coldWaveWarning?.level)
        assertEquals(WeatherAvailability.AVAILABLE, weather.availability)
    }

    @Test
    fun distantCachedWeatherCannotReplaceOlderLiveObservation() {
        val previous =
            response.copy(
                temperature = 12.0,
                humidity = 45.0,
                observedAt = now.minusSeconds(300).toString(),
            )
        val cached =
            CachedCurrentLocationWeather(
                latitude = 37.5,
                longitude = 127.0,
                weather = previous,
                sessionOwner = "session",
            )
        val selected =
            selectWeatherForCache(
                response,
                cached,
                38.5,
                127.0,
                now,
            )
        val weather = resolveReturnedWeather(response, selected.weather, now)
        assertEquals(8.7, weather.temperature!!, 0.0)
        assertEquals(WeatherAvailability.AVAILABLE, weather.availability)
        assertEquals(now.toString(), weather.serverReceivedAt)
    }

    @Test
    fun unavailableResponsePreservesFreshSameSessionNearbyCache() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())
        val unavailable =
            CurrentLocationWeatherResponseDto(
                temperature = 99.0,
                observedAt = now.toString(),
                availability = "UNAVAILABLE",
                errorCode = "NO_DATA",
            )

        val result =
            resolveUnavailableWeather(
                unavailable,
                cached,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.CACHED, result.availability)
        assertEquals(12.0, result.temperature!!, 0.0)
        assertEquals(cached.weather.observedAt, result.observedAt)
    }

    @Test
    fun unknownResponsePreservesFreshSameSessionNearbyCache() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())
        val unknown = CurrentLocationWeatherResponseDto(availability = "FUTURE_STATUS")

        val result =
            resolveUnavailableWeather(
                unknown,
                cached,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.CACHED, result.availability)
        assertEquals(12.0, result.temperature!!, 0.0)
    }

    @Test
    fun unavailableResponseWithoutCacheKeepsUnavailableResult() {
        val unavailable =
            CurrentLocationWeatherResponseDto(
                temperature = 99.0,
                humidity = 98.0,
                apparentTemperature = 100.0,
                heatLevel = 4,
                observedAt = now.toString(),
                skyStatus = "RAIN",
                temperatureDelta = 5.0,
                availability = "UNAVAILABLE",
                errorCode = "NO_DATA",
            )

        val result =
            resolveUnavailableWeather(
                unavailable,
                cached = null,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.UNAVAILABLE, result.availability)
        assertNull(result.temperature)
        assertNull(result.humidity)
        assertNull(result.apparentTemperature)
        assertNull(result.heatLevel)
        assertNull(result.observedAt)
        assertNull(result.skyStatus)
        assertNull(result.temperatureDelta)
        assertTrue(result.fieldProvenance.observedAtByField.isEmpty())
        assertEquals("NO_DATA", (result.unavailableReason as WeatherUnavailableReason.ServerReported).code)
    }

    @Test
    fun unknownResponseWithoutCacheDoesNotExposeUnavailableWeatherValues() {
        val unknown =
            CurrentLocationWeatherResponseDto(
                temperature = 99.0,
                humidity = 98.0,
                observedAt = now.toString(),
                availability = "FUTURE_STATUS",
            )

        val result =
            resolveUnavailableWeather(
                unknown,
                cached = null,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.UNKNOWN, result.availability)
        assertNull(result.temperature)
        assertNull(result.humidity)
        assertNull(result.observedAt)
    }

    @Test
    fun unavailableMainWeatherCanPreserveIndependentFreshValidColdWaveWarning() {
        listOf("AVAILABLE", "CACHED").forEach { availability ->
            val warning =
                ColdWaveWarningDto(
                    level = "WARNING",
                    availability = availability,
                    fetchedAt = now.minusSeconds(300).toString(),
                )
            val unavailable =
                CurrentLocationWeatherResponseDto(
                    temperature = 99.0,
                    observedAt = now.toString(),
                    availability = "UNAVAILABLE",
                    coldWaveWarning = warning,
                )

            val result =
                resolveUnavailableWeather(
                    unavailable,
                    cached = null,
                    requestedLatitude = 37.5,
                    requestedLongitude = 127.0,
                    currentSessionOwner = "session",
                    isRequestSessionCurrent = true,
                    now = now,
                )

            assertNull(result.temperature)
            assertNull(result.observedAt)
            assertEquals(ColdWaveLevel.WARNING, result.coldWaveWarning?.level)
            assertEquals(warning.fetchedAt, result.coldWaveWarning?.fetchedAt)
        }
    }

    @Test
    fun unavailableMainWeatherDoesNotExposeInvalidColdWaveWarnings() {
        val invalidWarnings =
            listOf(
                ColdWaveWarningDto(availability = "UNAVAILABLE", fetchedAt = now.toString()),
                ColdWaveWarningDto(availability = "UNKNOWN", fetchedAt = now.toString()),
                ColdWaveWarningDto(availability = "AVAILABLE", fetchedAt = now.minusSeconds(3_601).toString()),
                ColdWaveWarningDto(availability = "CACHED", fetchedAt = now.plusSeconds(1).toString()),
            )

        invalidWarnings.forEach { warning ->
            val result =
                resolveUnavailableWeather(
                    CurrentLocationWeatherResponseDto(
                        availability = "UNAVAILABLE",
                        coldWaveWarning = warning,
                    ),
                    cached = null,
                    requestedLatitude = 37.5,
                    requestedLongitude = 127.0,
                    currentSessionOwner = "session",
                    isRequestSessionCurrent = true,
                    now = now,
                )

            assertNull(result.coldWaveWarning)
        }
    }

    @Test
    fun unavailableResponseDoesNotRestoreExpiredCache() {
        val expired = cachedWeather(observedAt = now.minusSeconds(3_601).toString())
        val unavailable = CurrentLocationWeatherResponseDto(availability = "UNAVAILABLE")

        val result =
            resolveUnavailableWeather(
                unavailable,
                expired,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.UNAVAILABLE, result.availability)
        assertNull(result.temperature)
    }

    @Test
    fun unavailableResponseDoesNotRestoreCacheOutsideOneKilometer() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())
        val unavailable = CurrentLocationWeatherResponseDto(availability = "UNAVAILABLE")

        val result =
            resolveUnavailableWeather(
                unavailable,
                cached,
                requestedLatitude = 38.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.UNAVAILABLE, result.availability)
        assertNull(result.temperature)
    }

    @Test
    fun unavailableResponseDoesNotRestoreCacheFromAnotherSession() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())
        val unavailable = CurrentLocationWeatherResponseDto(availability = "UNAVAILABLE")

        val result =
            resolveUnavailableWeather(
                unavailable,
                cached,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "other-session",
                isRequestSessionCurrent = true,
                now = now,
            )

        assertEquals(WeatherAvailability.UNAVAILABLE, result.availability)
        assertNull(result.temperature)
    }

    @Test
    fun unavailableResponseDoesNotRestoreCacheAfterRequestSessionChanges() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())
        val unavailable = CurrentLocationWeatherResponseDto(availability = "UNAVAILABLE")

        val result =
            resolveUnavailableWeather(
                unavailable,
                cached,
                requestedLatitude = 37.5,
                requestedLongitude = 127.0,
                currentSessionOwner = "session",
                isRequestSessionCurrent = false,
                now = now,
            )

        assertEquals(WeatherAvailability.UNAVAILABLE, result.availability)
        assertNull(result.temperature)
    }

    @Test
    fun lastCachedWeatherIsReturnedOnlyForCurrentSessionAndBeforeSixtyMinutes() {
        val cached = cachedWeather(observedAt = now.minusSeconds(300).toString())

        val currentSessionWeather = resolveLastCachedWeatherForSession(cached, "session", true, now)
        val otherSessionWeather = resolveLastCachedWeatherForSession(cached, "other-session", true, now)
        val changedSessionWeather = resolveLastCachedWeatherForSession(cached, "session", false, now)
        val expiredWeather =
            resolveLastCachedWeatherForSession(cached, "session", true, now.plusSeconds(3_300))

        assertEquals(WeatherAvailability.CACHED, currentSessionWeather?.availability)
        assertEquals(12.0, currentSessionWeather?.temperature!!, 0.0)
        assertNull(otherSessionWeather)
        assertNull(changedSessionWeather)
        assertNull(expiredWeather)
    }

    private fun cachedWeather(observedAt: String) =
        CachedCurrentLocationWeather(
            latitude = 37.5,
            longitude = 127.0,
            weather = response.copy(temperature = 12.0, observedAt = observedAt),
            sessionOwner = "session",
        )
}
