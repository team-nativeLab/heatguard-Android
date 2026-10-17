package com.nativelap.heartguard.data.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PUT /api/v1/auth/team/password 요청 본문이다. 필드 이름은 명세에 없어 2026-09-28 실서버 검증 오류 응답으로 확인했다. */
@Serializable
data class ChangeWorkerPasswordRequestDto(
    @SerialName("currentPassword")
    val currentPassword: String,
    @SerialName("newPassword")
    val newPassword: String,
)
