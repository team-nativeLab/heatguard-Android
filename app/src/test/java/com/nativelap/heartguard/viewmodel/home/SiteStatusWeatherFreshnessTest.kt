package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.core.location.CurrentCoordinates
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
        assertEquals(12.0, afterOneMinute.recordTemperature!!, 0.0)
        assertEquals(42.0, afterOneMinute.recordHumidity!!, 0.0)
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

    @Test
    fun newerMemorySurvivesOlderCachedResponseOnlyForSameLocationSessionAndFreshness() {
        val now = Instant.parse("2026-11-11T01:00:00Z")
        val location = CurrentCoordinates(37.5, 127.0)
        val memory =
            CurrentLocationWeather(
                temperature = 12.0,
                humidity = 42.0,
                apparentTemperature = 11.0,
                heatLevel = 1,
                observedAt = now.minusSeconds(60).toString(),
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.AVAILABLE,
            )
        val older =
            memory.copy(
                observedAt = now.minusSeconds(120).toString(),
                availability = WeatherAvailability.CACHED,
            )
        val allowed = canUseWeatherMemory(memory, location, location, 1L, 1L, now)
        assertTrue(preferWeatherMemory(memory, older, allowed, now))
        assertFalse(preferWeatherMemory(memory, memory.copy(observedAt = now.toString()), allowed, now))
        val differentSession = canUseWeatherMemory(memory, location, location, 1L, 2L, now)
        assertFalse(preferWeatherMemory(memory, older, differentSession, now))
        val differentLocation = canUseWeatherMemory(memory, location, CurrentCoordinates(37.6, 127.0), 1L, 1L, now)
        assertFalse(preferWeatherMemory(memory, older, differentLocation, now))
        val expiredAt = now.plusSeconds(3_540)
        val expired = canUseWeatherMemory(memory, location, location, 1L, 1L, expiredAt)
        assertFalse(preferWeatherMemory(memory, older, expired, expiredAt))
    }

    @Test
    fun olderMainWeatherCanUpdateColdWaveIndependently() {
        val now = Instant.parse("2026-11-11T01:00:00Z")
        val oldWarning =
            ColdWaveWarning(
                level = ColdWaveLevel.ADVISORY,
                availability = ColdWaveAvailability.CACHED,
                areaCode = null,
                areaName = null,
                issuedAt = null,
                effectiveAt = null,
                fetchedAt = now.minusSeconds(300).toString(),
                isStale = true,
                errorCode = null,
                source = null,
            )
        val memory =
            CurrentLocationWeather(
                temperature = 12.0,
                humidity = 42.0,
                apparentTemperature = null,
                heatLevel = null,
                observedAt = now.minusSeconds(60).toString(),
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.AVAILABLE,
                coldWaveWarning = oldWarning,
            )
        val newerWarning =
            oldWarning.copy(
                level = ColdWaveLevel.WARNING,
                availability = ColdWaveAvailability.AVAILABLE,
                fetchedAt = now.minusSeconds(10).toString(),
                isStale = false,
            )
        val olderResponse =
            memory.copy(
                temperature = 10.0,
                observedAt = now.minusSeconds(120).toString(),
                coldWaveWarning = newerWarning,
            )
        val merged = preserveMemoryWeather(memory, listOf(olderResponse), now)
        assertEquals(memory.temperature, merged.temperature)
        assertEquals(memory.observedAt, merged.observedAt)
        assertEquals(newerWarning, merged.coldWaveWarning)
        val expiredWarning = newerWarning.copy(fetchedAt = now.minusSeconds(3_600).toString())
        val withExpiredWarning = olderResponse.copy(coldWaveWarning = expiredWarning)
        val withoutExpired = preserveMemoryWeather(memory, listOf(withExpiredWarning), now)
        assertEquals(oldWarning, withoutExpired.coldWaveWarning)
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

    @Test
    fun serverAvailableObservationOlderThanOneHourRemainsVisibleButNotRecordable() {
        val now = Instant.parse("2026-11-11T01:20:00Z")
        val weather =
            CurrentLocationWeather(
                temperature = 8.7,
                humidity = 77.0,
                apparentTemperature = 10.3,
                heatLevel = 0,
                observedAt = now.minusSeconds(4_800).toString(),
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.AVAILABLE,
                serverReceivedAt = now.toString(),
            )
        val status = HomeUiState.Success(emptyOverview()).toSiteStatusUiModel(weather, now)
        assertEquals("8.7", status.temperature)
        assertNull(status.recordTemperature)
        assertTrue(status.hasLocationWeatherValue)
        val expired = HomeUiState.Success(emptyOverview()).toSiteStatusUiModel(weather, now.plusSeconds(3_600))
        assertNull(expired.temperature)
        assertTrue(expired.isLocationWeatherExpired)
    }

    @Test
    fun oldLiveObservationStillRefreshesAgeAtTheNextObservationMinute() {
        val observedAt = Instant.parse("2026-11-11T00:00:00Z")
        val receivedAt = Instant.parse("2026-11-11T01:20:37Z")
        assertEquals(
            Instant.parse("2026-11-11T01:21:00Z"),
            nextLiveWeatherAgeRefresh(observedAt.toString(), receivedAt.toString(), receivedAt),
        )
        assertNull(nextLiveWeatherAgeRefresh(observedAt.toString(), receivedAt.toString(), receivedAt.plusSeconds(3_600)))
    }

    @Test
    fun oldLiveObservationCannotBeReusedAsMemoryAfterFailure() {
        val now = Instant.parse("2026-11-11T01:20:00Z")
        val location = CurrentCoordinates(37.5, 127.0)
        val weather =
            CurrentLocationWeather(
                temperature = 8.7,
                humidity = 77.0,
                apparentTemperature = null,
                heatLevel = null,
                observedAt = now.minusSeconds(4_800).toString(),
                skyStatus = null,
                temperatureDelta = null,
                availability = WeatherAvailability.AVAILABLE,
                serverReceivedAt = now.toString(),
            )
        assertFalse(canUseWeatherMemory(weather, location, location, 1L, 1L, now))
        assertFalse(canUseWeatherMemory(weather, location, location, 1L, 2L, now))
        assertFalse(canUseWeatherMemory(weather, location, CurrentCoordinates(37.6, 127.0), 1L, 1L, now))
    }
}
