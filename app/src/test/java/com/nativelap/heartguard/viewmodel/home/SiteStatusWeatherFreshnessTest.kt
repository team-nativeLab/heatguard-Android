package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import com.nativelap.heartguard.domain.site.model.ColdWaveWarning
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SiteStatusWeatherFreshnessTest {
    @Test
    fun refreshTimerUsesObservedAtMinuteBoundaryAndExpiration() {
        val timestamp = Instant.parse("2026-11-11T00:00:30Z")
        val timestampText = timestamp.toString()

        assertEquals(
            timestamp.plusSeconds(60),
            nextWeatherTimestampRefresh(Instant.parse("2026-11-11T00:01:20Z"), listOf(timestampText)),
        )
        assertEquals(
            timestamp.plusSeconds(3_600),
            nextWeatherTimestampRefresh(Instant.parse("2026-11-11T00:59:50Z"), listOf(timestampText)),
        )
        assertNull(
            nextWeatherTimestampRefresh(Instant.parse("2026-11-11T01:00:30Z"), listOf(timestampText)),
        )
    }

    @Test
    fun cachedWeatherAgeUpdatesAndExpiresAtSixtyMinutes() {
        val observedAt = Instant.parse("2026-11-11T00:00:00Z")
        val homeState = HomeUiState.Success(emptyOverview())
        val weather =
            CurrentLocationWeather(
                temperature = 12.0,
                humidity = 42.0,
                apparentTemperature = 11.0,
                heatLevel = 1,
                observedAt = observedAt.toString(),
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.CACHED,
            )

        val afterOneMinute = homeState.toSiteStatusUiModel(weather, observedAt.plusSeconds(60))
        val atSixtyMinutes = homeState.toSiteStatusUiModel(weather, observedAt.plusSeconds(3_600))

        assertEquals(1L, afterOneMinute.weatherAgeMinutes)
        assertTrue(afterOneMinute.isLocationWeatherCached)
        assertEquals(60L, atSixtyMinutes.weatherAgeMinutes)
        assertFalse(atSixtyMinutes.isLocationWeatherCached)
        assertTrue(atSixtyMinutes.isLocationWeatherExpired)
        assertNull(atSixtyMinutes.recordTemperature)
    }

    @Test
    fun expiredMainWeatherShowsFailureStateWhileFreshColdWaveRemainsAvailable() {
        val now = Instant.parse("2026-11-11T01:00:00Z")
        val homeState = HomeUiState.Success(emptyOverview())
        val weather =
            CurrentLocationWeather(
                temperature = null,
                humidity = null,
                apparentTemperature = null,
                heatLevel = null,
                observedAt = null,
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.CACHED,
                coldWaveWarning =
                    ColdWaveWarning(
                        level = ColdWaveLevel.WARNING,
                        availability = ColdWaveAvailability.CACHED,
                        areaCode = "11",
                        areaName = "서울",
                        issuedAt = null,
                        effectiveAt = null,
                        fetchedAt = now.minusSeconds(60).toString(),
                        isStale = true,
                        errorCode = null,
                        source = "cache",
                    ),
            )

        val status = homeState.toSiteStatusUiModel(weather, now)

        assertTrue(status.isLocationWeatherExpired)
        assertFalse(status.isColdWaveWarningUnavailable)
        assertEquals(ColdWaveLevel.WARNING, status.coldWaveWarning?.level)
    }

    private fun emptyOverview() =
        TeamSiteOverview(
            teamName = null,
            workplace = null,
            siteName = null,
            managerPhoneNumber = null,
            currentTemperature = null,
            humidity = null,
            apparentTemperature = null,
            heatLevel = null,
            checkTimes = emptyList(),
            hasActiveEmergencyCall = false,
        )
}
