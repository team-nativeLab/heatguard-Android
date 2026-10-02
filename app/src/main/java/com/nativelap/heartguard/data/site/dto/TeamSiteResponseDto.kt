package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /api/v1/team 응답의 data 필드다.
 * 예: {"date":"2026-09-28","team":{"teamId":"team_01","name":"철근팀","workplace":"3층 외벽"},
 * "site":{"siteId":"site_01","name":"서울현장","managerPhone":"010-1234-5678"},
 * "weather":{"temperature":null,"humidity":null,"apparentTemperature":null},"heatLevel":null,
 * "checkTimes":[],"checklistSummary":{"total":3,"completed":0},"activeEmergencyCall":null}
 * 명세 v0.1(2026-09-29)부터 company(본사 연락처), weather.skyStatus·temperatureDelta가 추가됐고 미입력은 null이다.
 * 실서버는 기상값이 입력되지 않으면 weather 측정값과 heatLevel을 null로, 관리자 연락처는 빈 문자열로 준다.
 * 값 하나가 비어 있다고 홈 전체가 역직렬화 실패로 떨어지지 않도록 서버가 생략·null로 줄 수 있는 필드는 nullable로 받는다. */
@Serializable
data class TeamSiteResponseDto(
    @SerialName("company")
    val company: CompanyDto? = null,
    @SerialName("team")
    val team: TeamDto,
    @SerialName("site")
    val site: SiteDto,
    @SerialName("weather")
    val weather: WeatherDto? = null,
    @SerialName("heatLevel")
    val heatLevel: Int? = null,
    @SerialName("checkTimes")
    val checkTimes: List<String> = emptyList(),
    // 체크리스트 요약은 홈 화면에 표시하는 UI가 없어 존재 여부만 파싱하고 도메인 모델로는 매핑하지 않는다.
    @SerialName("checklistSummary")
    val checklistSummary: ChecklistSummaryDto? = null,
    @SerialName("activeEmergencyCall")
    val activeEmergencyCall: ActiveEmergencyCallDto? = null,
)

@Serializable
data class CompanyDto(
    @SerialName("companyId")
    val companyId: String? = null,
    @SerialName("name")
    val name: String? = null,
    // 본사 관리자가 등록한 연락처다. 해제하면 null이 된다.
    @SerialName("phone")
    val phone: String? = null,
)

@Serializable
data class TeamDto(
    @SerialName("teamId")
    val teamId: String,
    @SerialName("name")
    val name: String? = null,
    // 팀의 작업 위치(예: "3층 외벽")다. 기록 응답에는 위치가 없어 화면은 이 값을 현재 작업 위치로 쓴다.
    @SerialName("workplace")
    val workplace: String? = null,
)

@Serializable
data class SiteDto(
    @SerialName("siteId")
    val siteId: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("managerPhone")
    val managerPhone: String? = null,
)

@Serializable
data class WeatherDto(
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("humidity")
    val humidity: Double? = null,
    @SerialName("apparentTemperature")
    val apparentTemperature: Double? = null,
    // 현장관리자가 수동 입력한 하늘 상태다(CLEAR·PARTLY_CLOUDY·CLOUDY·RAIN·SNOW). 새 값에 대비해 문자열로 받는다.
    @SerialName("skyStatus")
    val skyStatus: String? = null,
    // 직전 측정 대비 온도 변화량(°C)이다. 비교할 이전 측정이 없으면 null이다.
    @SerialName("temperatureDelta")
    val temperatureDelta: Double? = null,
)

@Serializable
data class ChecklistSummaryDto(
    @SerialName("total")
    val total: Int? = null,
    @SerialName("completed")
    val completed: Int? = null,
)

@Serializable
data class ActiveEmergencyCallDto(
    @SerialName("callId")
    val callId: String? = null,
    @SerialName("status")
    val status: String? = null,
)
