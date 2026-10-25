package com.nativelap.heartguard.data.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** PATCH /api/v1/auth/team/me 요청 본문이다.
 * 서버는 name·email·phone 중 하나 이상을 요구하지만(없으면 400 VALIDATION_ERROR),
 * Figma 24_내정보수정에서 편집 가능한 값은 이름뿐이라 이름만 보낸다. 이메일은 로그인 아이디라 변경하지 않는다.
 * [version]이 null이면 필드를 보내지 않는다. */
@Serializable
data class UpdateWorkerProfileRequestDto(
    @SerialName("name")
    val name: String,
    @SerialName("version")
    val version: Long? = null,
)
