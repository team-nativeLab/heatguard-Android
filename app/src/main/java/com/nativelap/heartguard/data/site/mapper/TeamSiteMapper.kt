package com.nativelap.heartguard.data.site.mapper

import com.nativelap.heartguard.data.site.dto.TeamSiteResponseDto
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview

internal fun TeamSiteResponseDto.toDomain(): TeamSiteOverview = TeamSiteOverview(
    teamName = team.name,
    siteName = site.name,
    managerPhoneNumber = site.managerPhone,
    currentTemperature = weather.temperature,
    humidity = weather.humidity,
    apparentTemperature = weather.apparentTemperature,
    heatLevel = heatLevel,
    checkTimes = checkTimes,
    hasActiveEmergencyCall = activeEmergencyCall?.callId != null,
)
