package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiteStatusUiModelTest {
    @Test
    fun mapsPositiveTemperatureDeltaAsIncreasing() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview(temperatureDelta = 2.5))
                .toSiteStatusUiModel()

        assertEquals("+2.5", siteStatus.temperatureDelta)
        assertTrue(siteStatus.isTemperatureIncreasing == true)
        assertFalse(siteStatus.isTemperatureDeltaZero)
    }

    @Test
    fun mapsNegativeTemperatureDeltaAsDecreasing() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview(temperatureDelta = -2.5))
                .toSiteStatusUiModel()

        assertEquals("-2.5", siteStatus.temperatureDelta)
        assertTrue(siteStatus.isTemperatureIncreasing == false)
        assertFalse(siteStatus.isTemperatureDeltaZero)
    }

    @Test
    fun mapsZeroTemperatureDeltaSeparatelyFromMissingValue() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview(temperatureDelta = 0.0))
                .toSiteStatusUiModel()

        assertEquals("0", siteStatus.temperatureDelta)
        assertNull(siteStatus.isTemperatureIncreasing)
        assertTrue(siteStatus.isTemperatureDeltaZero)
    }

    @Test
    fun keepsMissingTemperatureDeltaNeutral() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview(temperatureDelta = null))
                .toSiteStatusUiModel()

        assertNull(siteStatus.temperatureDelta)
        assertNull(siteStatus.isTemperatureIncreasing)
        assertFalse(siteStatus.isTemperatureDeltaZero)
    }

    @Test
    fun usesAvailableLocationWeatherForDisplayAndRecordDefaults() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview())
                .toSiteStatusUiModel(locationWeather(WeatherAvailability.AVAILABLE))

        assertEquals("31", siteStatus.temperature)
        assertEquals("55", siteStatus.humidity)
        assertEquals("30.8", siteStatus.apparentTemperature)
        assertEquals(0, siteStatus.heatLevel)
        assertEquals(SkyStatus.CLEAR, siteStatus.skyStatus)
        assertEquals("+2.1", siteStatus.temperatureDelta)
        assertEquals(31.0, siteStatus.recordTemperature)
        assertEquals(55.0, siteStatus.recordHumidity)
    }

    @Test
    fun usesCachedLocationWeather() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview())
                .toSiteStatusUiModel(locationWeather(WeatherAvailability.CACHED))

        assertEquals("31", siteStatus.temperature)
        assertEquals(31.0, siteStatus.recordTemperature)
    }

    @Test
    fun unavailableLocationWeatherKeepsTeamWeather() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview())
                .toSiteStatusUiModel(locationWeather(WeatherAvailability.UNAVAILABLE))

        assertEquals("22", siteStatus.temperature)
        assertEquals("40", siteStatus.humidity)
        assertEquals(22.0, siteStatus.recordTemperature)
        assertEquals(40.0, siteStatus.recordHumidity)
    }

    @Test
    fun unknownLocationWeatherKeepsTeamWeather() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview())
                .toSiteStatusUiModel(locationWeather(WeatherAvailability.UNKNOWN))

        assertEquals("22", siteStatus.temperature)
    }

    @Test
    fun availableLocationNullMeasurementsStayNull() {
        val siteStatus =
            HomeUiState
                .Success(siteOverview())
                .toSiteStatusUiModel(
                    locationWeather(
                        availability = WeatherAvailability.AVAILABLE,
                        temperature = null,
                        humidity = null,
                    ),
                )

        assertNull(siteStatus.temperature)
        assertNull(siteStatus.humidity)
        assertNull(siteStatus.recordTemperature)
        assertNull(siteStatus.recordHumidity)
    }

    private fun siteOverview(temperatureDelta: Double? = null) =
        TeamSiteOverview(
            teamName = null,
            workplace = null,
            siteName = null,
            managerPhoneNumber = null,
            currentTemperature = 22.0,
            humidity = 40.0,
            apparentTemperature = 22.5,
            heatLevel = 1,
            checkTimes = emptyList(),
            hasActiveEmergencyCall = false,
            skyStatus = SkyStatus.CLOUDY,
            temperatureDelta = temperatureDelta,
        )

    private fun locationWeather(
        availability: WeatherAvailability,
        temperature: Double? = 31.0,
        humidity: Double? = 55.0,
    ) = CurrentLocationWeather(
        temperature = temperature,
        humidity = humidity,
        apparentTemperature = 30.8,
        heatLevel = 0,
        observedAt = "2026-11-06T15:00:00+09:00",
        skyStatus = SkyStatus.CLEAR,
        temperatureDelta = 2.1,
        availability = availability,
    )
}
