package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CurrentLocationWeatherMapperTest {
    @Test
    fun requestEncodesCoordinatesAsJsonNumbers() {
        val request =
            CurrentLocationWeatherRequestDto(
                latitude = 37.5665,
                longitude = 126.978,
            )

        val requestJson = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject

        assertEquals(37.5665, requestJson.getValue("latitude").jsonPrimitive.double)
        assertEquals(126.978, requestJson.getValue("longitude").jsonPrimitive.double)
        assertFalse(requestJson.getValue("latitude").jsonPrimitive.isString)
        assertFalse(requestJson.getValue("longitude").jsonPrimitive.isString)
    }

    @Test
    fun mapsCachedWeatherAndPreservesNullableMeasurementValues() {
        val weather =
            CurrentLocationWeatherResponseDto(
                temperature = 31.0,
                humidity = null,
                apparentTemperature = 30.8,
                heatLevel = 0,
                observedAt = "2026-11-06T15:00:00+09:00",
                skyStatus = "CLEAR",
                availability = "CACHED",
            ).toDomain()

        assertEquals(WeatherAvailability.CACHED, weather.availability)
        assertEquals(31.0, weather.temperature)
        assertNull(weather.humidity)
        assertEquals(30.8, weather.apparentTemperature)
        assertEquals(SkyStatus.CLEAR, weather.skyStatus)
    }

    @Test
    fun mapsUnavailableAndUnrecognizedStatusSafely() {
        val weather =
            CurrentLocationWeatherResponseDto(
                temperature = null,
                humidity = null,
                skyStatus = "FOG",
                availability = "UNAVAILABLE",
            ).toDomain()

        assertEquals(WeatherAvailability.UNAVAILABLE, weather.availability)
        assertNull(weather.temperature)
        assertNull(weather.humidity)
        assertEquals(SkyStatus.UNKNOWN, weather.skyStatus)
    }

    @Test
    fun mapsUnknownAvailabilityToSafeUnknownState() {
        val weather =
            CurrentLocationWeatherResponseDto(
                temperature = 32.0,
                availability = "FUTURE_STATUS",
            ).toDomain()

        assertEquals(WeatherAvailability.UNKNOWN, weather.availability)
    }
}
