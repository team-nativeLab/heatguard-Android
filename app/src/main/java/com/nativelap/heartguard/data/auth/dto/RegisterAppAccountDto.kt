package com.nativelap.heartguard.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterAppAccountDto(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("confirmPassword")
    val confirmPassword: String,
    @SerialName("verificationCode")
    val verificationCode: String,
) {
    override fun toString(): String = "RegisterAppAccountDto(email=***, password=***, verificationCode=***)"
}

@Serializable
data class AppAccountRegistrationDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("email")
    val email: String,
    @SerialName("createdAt")
    val createdAt: String,
)
