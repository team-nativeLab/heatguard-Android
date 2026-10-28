package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
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

    private fun siteOverview(temperatureDelta: Double?) =
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
            temperatureDelta = temperatureDelta,
        )
}
