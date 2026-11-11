package com.nativelap.heartguard.view.route.home

import com.nativelap.heartguard.viewmodel.home.SiteStatusUiModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRouteWeatherFallbackTest {
    @Test
    fun freshTimestampWithoutVisibleWeatherDoesNotQualifyAsSiteFallback() {
        val siteStatus =
            SiteStatusUiModel(
                weatherObservedAt = "2026-11-11T06:00:00Z",
                heatLevel = 1,
                temperatureDelta = "+1",
            )

        assertFalse(siteStatus.hasWeatherValueToDisplay())
    }

    @Test
    fun visibleTemperatureQualifiesAsSiteFallback() {
        val siteStatus =
            SiteStatusUiModel(
                temperature = "22",
                weatherObservedAt = "2026-11-11T06:00:00Z",
            )

        assertTrue(siteStatus.hasWeatherValueToDisplay())
    }
}
