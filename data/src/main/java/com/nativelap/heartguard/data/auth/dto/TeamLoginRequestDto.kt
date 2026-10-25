package com.nativelap.heartguard.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamLoginRequestDto(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
) {
    // 로그인 자격 증명이 자동 생성된 toString()으로 로그에 노출되지 않게 한다.
    override fun toString(): String = "TeamLoginRequestDto(email=***, password=***)"
}
