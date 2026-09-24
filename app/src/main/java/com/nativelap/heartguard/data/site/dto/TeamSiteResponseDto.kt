package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.Serializable

/** GET /api/v1/t/{teamToken} 응답의 data 필드다.
 * 예: {"team":{"teamId":"team_01","name":"철근팀"},"site":{"siteId":"site_01","name":"서울현장",
 * "managerPhone":"010-1234-5678"},"weather":{"temperature":33.5,"humidity":62,"apparentTemperature":36.1},
 * "heatLevel":2,"checkTimes":["09:00","11:00"],"activeEmergencyCall":null} */
@Serializable
data class TeamSiteResponseDto(
    val team: TeamDto,
    val site: SiteDto,
    val weather: WeatherDto,
    val heatLevel: Int,
    val checkTimes: List<String> = emptyList(),
    // 체크리스트 요약은 응답 예시에 구체적인 필드 구성이 나와 있지 않아, 실제 값이 확인되기 전까지는
    // 존재 여부만 파싱하고 도메인 모델로는 매핑하지 않는다(홈 화면에 체크리스트 UI가 없음).
    val checklistSummary: ChecklistSummaryDto? = null,
    val activeEmergencyCall: ActiveEmergencyCallDto? = null,
)

@Serializable
data class TeamDto(
    val teamId: String,
    val name: String,
)

@Serializable
data class SiteDto(
    val siteId: String,
    val name: String,
    val managerPhone: String,
)

@Serializable
data class WeatherDto(
    val temperature: Double,
    val humidity: Double,
    val apparentTemperature: Double,
)

@Serializable
data class ChecklistSummaryDto(
    val total: Int? = null,
    val completed: Int? = null,
)

@Serializable
data class ActiveEmergencyCallDto(
    val callId: String? = null,
    val status: String? = null,
)
