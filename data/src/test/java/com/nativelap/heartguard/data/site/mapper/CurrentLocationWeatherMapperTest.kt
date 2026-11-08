package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherRequestDto
import com.nativelap.heartguard.data.site.dto.CurrentLocationWeatherResponseDto
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
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

    @Test
    fun mapsCurrentLocationColdWaveWarningAndCachedMetadata() {
        val weather =
            CurrentLocationWeatherResponseDto(
                availability = "CACHED",
                coldWaveWarning =
                    coldWaveWarningDto(
                        level = "ADVISORY",
                        availability = "CACHED",
                        isStale = true,
                    ),
            ).toDomain()

        assertEquals(ColdWaveLevel.ADVISORY, weather.coldWaveWarning?.level)
        assertEquals(ColdWaveAvailability.CACHED, weather.coldWaveWarning?.availability)
        assertEquals("L1100400", weather.coldWaveWarning?.areaCode)
        assertEquals("서울서북권", weather.coldWaveWarning?.areaName)
        assertEquals("2026-11-08T10:00:00+09:00", weather.coldWaveWarning?.issuedAt)
        assertEquals("2026-11-08T11:00:00+09:00", weather.coldWaveWarning?.effectiveAt)
        assertEquals("2026-11-08T12:00:00+09:00", weather.coldWaveWarning?.fetchedAt)
        assertEquals(true, weather.coldWaveWarning?.isStale)
        assertNull(weather.coldWaveWarning?.errorCode)
        assertEquals("KMA", weather.coldWaveWarning?.source)
    }

    @Test
    fun decodesColdWaveWarningAtCurrentLocationResponseRoot() {
        val weather =
            Json.decodeFromString<CurrentLocationWeatherResponseDto>(
                """{"coldWaveWarning":{"level":"WARNING","availability":"AVAILABLE","source":"KMA"}}""",
            )

        assertEquals(ColdWaveLevel.WARNING, weather.toDomain().coldWaveWarning?.level)
        assertEquals(ColdWaveAvailability.AVAILABLE, weather.toDomain().coldWaveWarning?.availability)
        assertEquals("KMA", weather.toDomain().coldWaveWarning?.source)
    }

    @Test
    fun doesNotMapNullOrUnknownColdWaveLevelsToNone() {
        val missingLevel =
            CurrentLocationWeatherResponseDto(
                coldWaveWarning = coldWaveWarningDto(level = null),
            ).toDomain()
        val unknownLevel =
            CurrentLocationWeatherResponseDto(
                coldWaveWarning = coldWaveWarningDto(level = "FUTURE_LEVEL"),
            ).toDomain()

        assertNull(missingLevel.coldWaveWarning?.level)
        assertEquals(ColdWaveLevel.UNKNOWN, unknownLevel.coldWaveWarning?.level)
        assertEquals(ColdWaveAvailability.AVAILABLE, unknownLevel.coldWaveWarning?.availability)
    }

    @Test
    fun mapsLocationRequiredSeparatelyFromUnavailable() {
        val weather =
            CurrentLocationWeatherResponseDto(
                coldWaveWarning = coldWaveWarningDto(availability = "LOCATION_REQUIRED"),
            ).toDomain()

        assertEquals(ColdWaveAvailability.LOCATION_REQUIRED, weather.coldWaveWarning?.availability)
    }

    @Test
    fun mapsUnknownColdWaveAvailabilityToUnknown() {
        val weather =
            CurrentLocationWeatherResponseDto(
                coldWaveWarning = coldWaveWarningDto(availability = "FUTURE_AVAILABILITY"),
            ).toDomain()

        assertEquals(ColdWaveAvailability.UNKNOWN, weather.coldWaveWarning?.availability)
    }

    private fun coldWaveWarningDto(
        level: String? = "WARNING",
        availability: String? = "AVAILABLE",
        isStale: Boolean? = false,
    ) = ColdWaveWarningDto(
        level = level,
        availability = availability,
        areaCode = "L1100400",
        areaName = "서울서북권",
        issuedAt = "2026-11-08T10:00:00+09:00",
        effectiveAt = "2026-11-08T11:00:00+09:00",
        fetchedAt = "2026-11-08T12:00:00+09:00",
        isStale = isStale,
        errorCode = null,
        source = "KMA",
    )
}
