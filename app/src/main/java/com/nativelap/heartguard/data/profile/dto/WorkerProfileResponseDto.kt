package com.nativelap.heartguard.data.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET·PATCH /api/v1/auth/team/me 응답의 data 필드다.
 * 예: {"userId":"usr_01","name":"홍길동","email":"worker01","phone":"010-0000-0000","role":"TEAM_MEMBER",
 * "scopeId":"team_01","siteId":"site_01","teamId":"team_01","version":1,"expiresAt":"2026-09-28T22:36:06+00:00"}
 * 명세에 필드 정의가 없어 2026-09-28 실서버 응답으로 확인한 구조다. 화면에 쓰지 않는 세션 필드는 받지 않는다. */
@Serializable
data class WorkerProfileResponseDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("phone")
    val phone: String? = null,
)
