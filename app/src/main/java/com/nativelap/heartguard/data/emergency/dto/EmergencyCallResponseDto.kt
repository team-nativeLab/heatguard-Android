package com.nativelap.heartguard.data.emergency.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 긴급호출 등록/상태조회 응답의 data 필드다. 두 API가 같은 모양을 공유한다.
 * 등록 예: {"callId":"call_01","status":"ACTIVE"}
 * 상태조회 예: {"callId":"call_01","status":"ACKNOWLEDGED","acknowledgedAt":"2026-08-07T10:15:00+09:00"} */
@Serializable
data class EmergencyCallResponseDto(
    @SerialName("callId")
    val callId: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("acknowledgedAt")
    val acknowledgedAt: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("cancelledAt")
    val cancelledAt: String? = null,
    @SerialName("completedAt")
    val completedAt: String? = null,
)
