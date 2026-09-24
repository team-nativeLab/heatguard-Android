package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.ActiveEmergencyCallDto
import com.nativelap.heartguard.data.site.dto.SiteDto
import com.nativelap.heartguard.data.site.dto.TeamDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.data.site.dto.WeatherDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamSiteMapperTest {

    @Test
    fun `activeEmergencyCall이 없으면 hasActiveEmergencyCall은 false다`() {
        val dto = teamSiteResponseDto(activeEmergencyCall = null)

        val overview = dto.toDomain()

        assertFalse(overview.hasActiveEmergencyCall)
    }

    @Test
    fun `activeEmergencyCall에 callId가 있으면 hasActiveEmergencyCall은 true다`() {
        val dto = teamSiteResponseDto(
            activeEmergencyCall = ActiveEmergencyCallDto(callId = "call_01", status = "ACTIVE"),
        )

        val overview = dto.toDomain()

        assertTrue(overview.hasActiveEmergencyCall)
    }

    @Test
    fun `team, site, weather 필드가 도메인 모델로 그대로 매핑된다`() {
        val dto = teamSiteResponseDto()

        val overview = dto.toDomain()

        assertEquals("철근팀", overview.teamName)
        assertEquals("서울현장", overview.siteName)
        assertEquals("010-1234-5678", overview.managerPhoneNumber)
        assertEquals(33.5, overview.currentTemperature, 0.0)
        assertEquals(62.0, overview.humidity, 0.0)
        assertEquals(36.1, overview.apparentTemperature, 0.0)
        assertEquals(2, overview.heatLevel)
        assertEquals(listOf("09:00", "11:00"), overview.checkTimes)
    }

    private fun teamSiteResponseDto(
        activeEmergencyCall: ActiveEmergencyCallDto? = null,
    ) = TeamSiteResponseDto(
        team = TeamDto(teamId = "team_01", name = "철근팀"),
        site = SiteDto(siteId = "site_01", name = "서울현장", managerPhone = "010-1234-5678"),
        weather = WeatherDto(temperature = 33.5, humidity = 62.0, apparentTemperature = 36.1),
        heatLevel = 2,
        checkTimes = listOf("09:00", "11:00"),
        activeEmergencyCall = activeEmergencyCall,
    )
}
