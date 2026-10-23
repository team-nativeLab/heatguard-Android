package com.nativelap.heartguard.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppAccountLoginRequestDto(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
) {
    override fun toString(): String = "AppAccountLoginRequestDto(email=***, password=***)"
}

@Serializable
data class AppAccountLoginResponseDto(
    @SerialName("accessToken")
    val accessToken: String,
) {
    override fun toString(): String = "AppAccountLoginResponseDto(accessToken=***)"
}
