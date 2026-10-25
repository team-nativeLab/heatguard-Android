package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteStatusUiModelTest {
    @Test
    fun `온도가 오르면 부호가 붙은 변화량과 상승 방향을 만든다`() {
        val siteStatus = HomeUiState.Success(overview(temperatureDelta = 3.2)).toSiteStatusUiModel()

        assertEquals("+3.2", siteStatus.temperatureDelta)
        assertEquals(true, siteStatus.isTemperatureIncreasing)
    }

    @Test
    fun `온도가 내리면 음수 변화량과 하강 방향을 만든다`() {
        val siteStatus = HomeUiState.Success(overview(temperatureDelta = -1.5)).toSiteStatusUiModel()

        assertEquals("-1.5", siteStatus.temperatureDelta)
        assertEquals(false, siteStatus.isTemperatureIncreasing)
    }

    @Test
    fun `변화량이 0이면 방향 없이 0을 보여준다`() {
        val siteStatus = HomeUiState.Success(overview(temperatureDelta = 0.0)).toSiteStatusUiModel()

        assertEquals("0", siteStatus.temperatureDelta)
        assertNull(siteStatus.isTemperatureIncreasing)
    }

    @Test
    fun `변화량이 없으면 값과 방향이 모두 null이다`() {
        val siteStatus = HomeUiState.Success(overview(temperatureDelta = null)).toSiteStatusUiModel()

        assertNull(siteStatus.temperatureDelta)
        assertNull(siteStatus.isTemperatureIncreasing)
    }

    @Test
    fun `본사 연락처와 하늘 상태를 그대로 전달한다`() {
        val siteStatus =
            HomeUiState
                .Success(
                    overview(
                        headquartersPhoneNumber = "02-000-0000",
                        skyStatus = SkyStatus.CLOUDY,
                    ),
                ).toSiteStatusUiModel()

        assertEquals("02-000-0000", siteStatus.headquartersPhoneNumber)
        assertEquals(SkyStatus.CLOUDY, siteStatus.skyStatus)
    }

    private fun overview(
        temperatureDelta: Double? = null,
        headquartersPhoneNumber: String? = null,
        skyStatus: SkyStatus? = null,
    ) = TeamSiteOverview(
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
        headquartersPhoneNumber = headquartersPhoneNumber,
        skyStatus = skyStatus,
        temperatureDelta = temperatureDelta,
    )
}
