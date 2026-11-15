package com.nativelap.heartguard.domain.auth.passwordreset.model

sealed interface PasswordResetResult<out Value> {
    data class Success<Value>(
        val value: Value,
    ) : PasswordResetResult<Value>

    data class Failure(
        val error: PasswordResetError,
    ) : PasswordResetResult<Nothing>
}

enum class PasswordResetError {
    Validation,
    InvalidCode,
    InvalidToken,
    RateLimited,
    ServiceUnavailable,
    Network,
    Generic,
}

data class PasswordResetEmailCode(
    val expiresAt: String,
)

data class PasswordResetVerification(
    val expiresAt: String,
    val resetToken: String,
) {
    override fun toString(): String = "PasswordResetVerification(expiresAt=$expiresAt, resetToken=[REDACTED])"
}

data class PasswordResetCompletion(
    val changedAt: String,
)
