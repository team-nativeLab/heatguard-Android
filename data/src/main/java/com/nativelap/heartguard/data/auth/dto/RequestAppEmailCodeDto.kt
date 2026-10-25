package com.nativelap.heartguard.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestAppEmailCodeDto(
    @SerialName("email")
    val email: String,
) {
    override fun toString(): String = "RequestAppEmailCodeDto(email=***)"
}

@Serializable
data class AppEmailCodeResponseDto(
    @SerialName("expiresAt")
    val expiresAt: String,
)
