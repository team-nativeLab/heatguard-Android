package com.nativelap.heartguard.data.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PUT /api/v1/auth/team/password 응답의 data 필드다. 예: {"changedAt":"2026-09-28T14:35:53+00:00"} */
@Serializable
data class ChangeWorkerPasswordResponseDto(
    @SerialName("changedAt")
    val changedAt: String? = null,
)
