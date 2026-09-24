package com.nativelap.heartguard.data.emergency.dto

import kotlinx.serialization.Serializable

/** POST /api/v1/t/{teamToken}/emergency-calls 요청 본문이다.
 * 예: {"message":"작업자 상태 확인 필요","clientOccurredAt":"2026-08-07T10:12:00+09:00"} */
@Serializable
data class EmergencyCallRequestDto(
    val message: String? = null,
    val clientOccurredAt: String,
)
