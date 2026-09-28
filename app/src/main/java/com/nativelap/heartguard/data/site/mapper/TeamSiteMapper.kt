package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview

// 서버가 빈 문자열로 준 값(예: 관리자 연락처 미등록)은 "값 없음"과 같게 취급해 화면이 "--"로 표시하게 한다.
internal fun TeamSiteResponseDto.toDomain(): TeamSiteOverview = TeamSiteOverview(
    teamName = team.name.nullIfBlank(),
    workplace = team.workplace.nullIfBlank(),
    siteName = site.name.nullIfBlank(),
    managerPhoneNumber = site.managerPhone.nullIfBlank(),
    currentTemperature = weather?.temperature,
    humidity = weather?.humidity,
    apparentTemperature = weather?.apparentTemperature,
    heatLevel = heatLevel,
    checkTimes = checkTimes,
    hasActiveEmergencyCall = activeEmergencyCall?.callId != null,
)

private fun String?.nullIfBlank(): String? = this?.takeIf { text -> text.isNotBlank() }
