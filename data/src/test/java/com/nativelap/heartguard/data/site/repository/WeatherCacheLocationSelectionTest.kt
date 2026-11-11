package com.nativelap.heartguard.data.site.repository

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.data.site.local.CachedCurrentLocationWeather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WeatherCacheLocationSelectionTest {
    @Test
    fun sameTimestampNewResponseUsesNewCoordinatesForRadiusCheck() {
        val now = Instant.parse("2026-11-11T01:00:00Z")
        val oldWeather =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                humidity = 55.0,
                observedAt = now.toString(),
                availability = "AVAILABLE",
            )
        val previous = CachedCurrentLocationWeather(0.0, 0.0, oldWeather, "session")
        val newWeather = oldWeather.copy(temperature = 13.0)
        val selected = selectWeatherForCache(newWeather, previous, 0.008, 0.0, now)

        assertTrue(isWithinWeatherCacheRadius(0.0, 0.0, 0.008, 0.0))
        assertEquals(0.008, selected.latitude, 0.0)
        assertEquals(13.0, selected.weather.temperature!!, 0.0)
        assertFalse(isWithinWeatherCacheRadius(selected.latitude, selected.longitude, -0.008, 0.0))
    }

    @Test
    fun partialResponseRetainsPreviousOrigin() {
        val now = Instant.parse("2026-11-11T01:00:00Z")
        val oldWeather =
            CurrentLocationWeatherResponseDto(
                temperature = 12.0,
                humidity = 55.0,
                observedAt = now.toString(),
                availability = "AVAILABLE",
            )
        val previous = CachedCurrentLocationWeather(0.0, 0.0, oldWeather, "session")
        val selected = selectWeatherForCache(oldWeather.copy(humidity = null), previous, 0.008, 0.0, now)

        assertEquals(0.0, selected.latitude, 0.0)
        assertEquals("CACHED", selected.weather.availability)
        assertFalse(isWithinWeatherCacheRadius(selected.latitude, selected.longitude, 0.016, 0.0))
    }
}
