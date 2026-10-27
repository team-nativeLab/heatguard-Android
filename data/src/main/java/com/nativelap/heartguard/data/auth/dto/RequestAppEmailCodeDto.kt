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

@Serializable
data class VerifyAppEmailCodeDto(
    @SerialName("email")
    val email: String,
    @SerialName("verificationCode")
    val verificationCode: String,
) {
    override fun toString(): String = "VerifyAppEmailCodeDto(email=***, verificationCode=***)"
}

@Serializable
data class AppEmailCodeVerificationDto(
    @SerialName("verified")
    val verified: Boolean,
    @SerialName("verificationToken")
    val verificationToken: String,
    @SerialName("expiresAt")
    val expiresAt: String,
) {
    override fun toString(): String = "AppEmailCodeVerificationDto(verificationToken=***)"
}
