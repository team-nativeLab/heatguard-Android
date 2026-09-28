package com.nativelap.heartguard.domain.site.model

/** 작업자 홈 조회(GET /api/v1/team) 응답을 화면이 바로 쓸 수 있게 정리한 도메인 모델이다. */
data class TeamSiteOverview(
    val teamName: String,
    val siteName: String,
    val managerPhoneNumber: String,
    val currentTemperature: Double?,
    val humidity: Double?,
    val apparentTemperature: Double?,
    val heatLevel: Int,
    val checkTimes: List<String>,
    val hasActiveEmergencyCall: Boolean,
)
