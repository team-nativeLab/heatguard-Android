package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.ActiveEmergencyCallDto
import com.nativelap.heartguard.data.site.dto.CompanyDto
import com.nativelap.heartguard.data.site.dto.SiteDto
import com.nativelap.heartguard.data.site.dto.TeamDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.data.site.dto.WeatherDto
import com.nativelap.heartguard.domain.site.model.SkyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals("3층 외벽", overview.workplace)
        assertEquals("서울현장", overview.siteName)
        assertEquals("010-1234-5678", overview.managerPhoneNumber)
        assertEquals(33.5, overview.currentTemperature ?: error("temperature missing"), 0.0)
        assertEquals(62.0, overview.humidity ?: error("humidity missing"), 0.0)
        assertEquals(36.1, overview.apparentTemperature ?: error("apparent temperature missing"), 0.0)
        assertEquals(2, overview.heatLevel)
        assertEquals(listOf("09:00", "11:00"), overview.checkTimes)
    }

    @Test
    fun `기상 미입력 시 측정값 null이 도메인 모델에 유지된다`() {
        val overview = teamSiteResponseDto(
            weather = WeatherDto(),
        ).toDomain()

        assertEquals(null, overview.currentTemperature)
        assertEquals(null, overview.humidity)
        assertEquals(null, overview.apparentTemperature)
    }

    @Test
    fun `서버가 빈 문자열로 준 관리자 연락처와 작업 위치는 null로 정규화된다`() {
        val overview = teamSiteResponseDto(
            team = TeamDto(teamId = "team_01", name = "철근팀", workplace = ""),
            site = SiteDto(siteId = "site_01", name = "서울현장", managerPhone = ""),
        ).toDomain()

        assertNull(overview.managerPhoneNumber)
        assertNull(overview.workplace)
    }

    @Test
    fun `heatLevel과 weather가 null이어도 매핑된다`() {
        val overview = teamSiteResponseDto(
            weather = null,
            heatLevel = null,
        ).toDomain()

        assertNull(overview.heatLevel)
        assertNull(overview.currentTemperature)
    }

    @Test
    fun `본사 연락처와 하늘 상태, 온도 변화량이 도메인 모델로 매핑된다`() {
        val overview = teamSiteResponseDto(
            company = CompanyDto(companyId = "cmp_01", name = "이음", phone = "02-000-0000"),
            weather = WeatherDto(skyStatus = "RAIN", temperatureDelta = 3.2),
        ).toDomain()

        assertEquals("02-000-0000", overview.headquartersPhoneNumber)
        assertEquals(SkyStatus.RAIN, overview.skyStatus)
        assertEquals(3.2, overview.temperatureDelta ?: error("delta missing"), 0.0)
    }

    @Test
    fun `알 수 없는 하늘 상태는 UNKNOWN, 빈 본사 연락처와 company 누락은 null이다`() {
        val unknownSky = teamSiteResponseDto(
            company = CompanyDto(phone = ""),
            weather = WeatherDto(skyStatus = "FOG"),
        ).toDomain()
        val missingCompany = teamSiteResponseDto(company = null).toDomain()

        assertEquals(SkyStatus.UNKNOWN, unknownSky.skyStatus)
        assertNull(unknownSky.headquartersPhoneNumber)
        assertNull(missingCompany.headquartersPhoneNumber)
        assertNull(missingCompany.skyStatus)
    }

    private fun teamSiteResponseDto(
        company: CompanyDto? = null,
        activeEmergencyCall: ActiveEmergencyCallDto? = null,
        weather: WeatherDto? = WeatherDto(temperature = 33.5, humidity = 62.0, apparentTemperature = 36.1),
        heatLevel: Int? = 2,
        team: TeamDto = TeamDto(teamId = "team_01", name = "철근팀", workplace = "3층 외벽"),
        site: SiteDto = SiteDto(siteId = "site_01", name = "서울현장", managerPhone = "010-1234-5678"),
    ) = TeamSiteResponseDto(
        company = company,
        team = team,
        site = site,
        weather = weather,
        heatLevel = heatLevel,
        checkTimes = listOf("09:00", "11:00"),
        activeEmergencyCall = activeEmergencyCall,
    )
}
