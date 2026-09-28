package com.nativelap.heartguard.domain.site.model

/** 작업자 홈 조회(GET /api/v1/team) 응답을 화면이 바로 쓸 수 있게 정리한 도메인 모델이다.
 * 서버가 비워 준 문자열 값(팀명·작업 위치·관리자 연락처 등)은 null로 정규화한다. */
data class TeamSiteOverview(
    val teamName: String?,
    val workplace: String?,
    val siteName: String?,
    val managerPhoneNumber: String?,
    val currentTemperature: Double?,
    val humidity: Double?,
    val apparentTemperature: Double?,
    val heatLevel: Int?,
    val checkTimes: List<String>,
    val hasActiveEmergencyCall: Boolean,
)
