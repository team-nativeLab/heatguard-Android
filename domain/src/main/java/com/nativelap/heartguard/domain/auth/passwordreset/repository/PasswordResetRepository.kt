package com.nativelap.heartguard.domain.auth.passwordreset.repository

import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetCompletion
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetEmailCode
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetVerification

interface PasswordResetRepository {
    suspend fun requestEmailCode(email: String): PasswordResetResult<PasswordResetEmailCode>

    suspend fun verifyEmailCode(
        email: String,
        verificationCode: String,
    ): PasswordResetResult<PasswordResetVerification>

    suspend fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String,
        confirmPassword: String,
    ): PasswordResetResult<PasswordResetCompletion>
}
