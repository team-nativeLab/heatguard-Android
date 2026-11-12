package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.local.CachedCurrentLocationWeather
import com.nativelap.heartguard.data.site.local.expireWeatherParts
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class LiveWeatherResponseTest {
    private val now = Instant.parse("2026-11-11T14:20:00Z")
    private val response =
        CurrentLocationWeatherResponseDto(
            temperature = 8.7,
            humidity = 77.0,
            observedAt = now.minusSeconds(4_800).toString(),
            availability = "AVAILABLE",
            isStale = false,
        )

    @Test
    fun availableLiveResponsePreservesOlderServerObservation() {
        val weather = response.toDisplayableLiveWeather(now)
        assertNotNull(weather)
        assertEquals(WeatherAvailability.AVAILABLE, weather?.availability)
        assertEquals(response.observedAt, weather?.observedAt)
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
                observedAt = now.minusSeconds(600).toString(),
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
                response.expireWeatherParts(now),
                cached,
                37.5,
                127.0,
                now,
            )
        val weather = resolveReturnedWeather(response, selected.weather, now)
        assertEquals(12.0, weather.temperature!!, 0.0)
        assertEquals(45.0, weather.humidity!!, 0.0)
        assertEquals(WeatherAvailability.CACHED, weather.availability)
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
                incoming.expireWeatherParts(now),
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
                observedAt = now.minusSeconds(600).toString(),
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
                response.expireWeatherParts(now),
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
}
