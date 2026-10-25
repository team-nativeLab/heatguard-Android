package com.nativelap.heartguard.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** user/team/site는 현재 앱에서 소비하지 않고, 로그인에 필요한 accessToken만 저장한다. */
@Serializable
data class TeamLoginResponseDto(
    @SerialName("accessToken")
    val accessToken: String,
) {
    // 인증 토큰은 자동 생성된 toString()으로도 출력되지 않게 한다.
    override fun toString(): String = "TeamLoginResponseDto(accessToken=***)"
}
