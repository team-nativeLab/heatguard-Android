package com.nativelap.heartguard.data.auth.passwordreset.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class RequestPasswordResetEmailCodeDto(
    @SerialName("email") val email: String,
)

@Serializable data class VerifyPasswordResetEmailCodeDto(
    @SerialName("email") val email: String,
    @SerialName("verificationCode") val verificationCode: String,
) {
    override fun toString(): String = "VerifyPasswordResetEmailCodeDto(email=[REDACTED], verificationCode=[REDACTED])"
}

@Serializable data class ResetPasswordDto(
    @SerialName("email") val email: String,
    @SerialName("resetToken") val resetToken: String,
    @SerialName("newPassword") val newPassword: String,
    @SerialName("confirmPassword") val confirmPassword: String,
) {
    override fun toString(): String =
        "ResetPasswordDto(email=[REDACTED], resetToken=[REDACTED], newPassword=[REDACTED], confirmPassword=[REDACTED])"
}

@Serializable data class PasswordResetEmailCodeResponseDto(
    @SerialName("expiresAt") val expiresAt: String,
)

@Serializable data class PasswordResetVerificationResponseDto(
    @SerialName("resetToken") val resetToken: String,
    @SerialName("expiresAt") val expiresAt: String,
) {
    override fun toString(): String =
        "PasswordResetVerificationResponseDto(resetToken=[REDACTED], expiresAt=$expiresAt)"
}

@Serializable data class PasswordResetCompletionResponseDto(
    @SerialName("passwordChanged") val passwordChanged: Boolean,
    @SerialName("changedAt") val changedAt: String,
)
