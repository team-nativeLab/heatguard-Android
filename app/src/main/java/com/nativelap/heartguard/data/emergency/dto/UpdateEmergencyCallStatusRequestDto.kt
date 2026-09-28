package com.nativelap.heartguard.data.emergency.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PATCH /api/v1/team/emergency-calls/{callId} 요청 본문이다. */
@Serializable
data class UpdateEmergencyCallStatusRequestDto(
    @SerialName("status")
    val status: String,
)
