package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.ColdWaveWarningDto
import com.nativelap.heartguard.data.site.dto.SiteDto
import com.nativelap.heartguard.data.site.dto.TeamDto
import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.data.site.dto.WeatherDto
import com.nativelap.heartguard.domain.site.model.ColdWaveAvailability
import com.nativelap.heartguard.domain.site.model.ColdWaveLevel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamSiteMapperTest {
    @Test
    fun mapsNestedTeamColdWaveWarning() {
        val overview =
            teamSiteResponse(
                coldWaveWarning =
                    ColdWaveWarningDto(
                        level = "WARNING",
                        availability = "AVAILABLE",
                        areaCode = "L1100400",
                        areaName = "서울서북권",
                        issuedAt = "2026-11-08T10:00:00+09:00",
                        effectiveAt = "2026-11-08T11:00:00+09:00",
                        fetchedAt = "2026-11-08T12:00:00+09:00",
                        isStale = false,
                        source = "KMA",
                    ),
            ).toDomain()

        assertEquals(ColdWaveLevel.WARNING, overview.coldWaveWarning?.level)
        assertEquals(ColdWaveAvailability.AVAILABLE, overview.coldWaveWarning?.availability)
        assertEquals("L1100400", overview.coldWaveWarning?.areaCode)
        assertEquals(false, overview.coldWaveWarning?.isStale)
        assertEquals("KMA", overview.coldWaveWarning?.source)
    }

    @Test
    fun decodesColdWaveWarningNestedUnderTeamWeather() {
        val response =
            Json.decodeFromString<TeamSiteResponseDto>(
                """{"team":{"teamId":"team_1"},"site":{"siteId":"site_1"},"weather":{"coldWaveWarning":{"level":"ADVISORY","availability":"AVAILABLE"}}}""",
            )

        assertEquals(ColdWaveLevel.ADVISORY, response.toDomain().coldWaveWarning?.level)
        assertEquals(ColdWaveAvailability.AVAILABLE, response.toDomain().coldWaveWarning?.availability)
    }

    @Test
    fun missingTeamWeatherAndColdWaveWarningStayAbsent() {
        val overview =
            teamSiteResponse(
                coldWaveWarning = null,
                weather = null,
            ).toDomain()

        assertNull(overview.coldWaveWarning)
        assertNull(overview.currentTemperature)
    }

    @Test
    fun keepsNullAndUnknownTeamColdWaveLevelsDistinctFromNone() {
        val noLevel =
            teamSiteResponse(
                coldWaveWarning = ColdWaveWarningDto(level = null),
            ).toDomain()
        val unrecognizedLevel =
            teamSiteResponse(
                coldWaveWarning = ColdWaveWarningDto(level = "FUTURE_LEVEL"),
            ).toDomain()

        assertNull(noLevel.coldWaveWarning?.level)
        assertEquals(ColdWaveLevel.UNKNOWN, unrecognizedLevel.coldWaveWarning?.level)
    }

    private fun teamSiteResponse(
        coldWaveWarning: ColdWaveWarningDto?,
        weather: WeatherDto? = WeatherDto(coldWaveWarning = coldWaveWarning),
    ) = TeamSiteResponseDto(
        team = TeamDto(teamId = "team_1"),
        site = SiteDto(siteId = "site_1"),
        weather = weather,
    )
}
