package com.nativelap.heartguard.data.emergency.mapper

import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus

internal fun EmergencyCallResponseDto.toDomain(): EmergencyCallStatus = EmergencyCallStatus(
    callId = callId,
    state = when (status) {
        "ACTIVE" -> EmergencyCallState.ACTIVE
        "ACKNOWLEDGED" -> EmergencyCallState.ACKNOWLEDGED
        else -> EmergencyCallState.NONE
    },
    acknowledgedAt = acknowledgedAt,
)
