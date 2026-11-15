package com.nativelap.heartguard.domain.auth.passwordreset.usecase

import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetCompletion
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetEmailCode
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetVerification
import com.nativelap.heartguard.domain.auth.passwordreset.repository.PasswordResetRepository
import com.nativelap.heartguard.domain.profile.usecase.ChangeWorkerPasswordUseCase
import java.util.Locale
import javax.inject.Inject

class RequestPasswordResetEmailCodeUseCase
    @Inject
    constructor(
        private val repository: PasswordResetRepository,
    ) {
        suspend operator fun invoke(email: String): PasswordResetResult<PasswordResetEmailCode> =
            if (email.trim().isValidEmail()) {
                repository.requestEmailCode(email.trim().lowercase(Locale.ROOT))
            } else {
                PasswordResetResult.Failure(PasswordResetError.Validation)
            }
    }

class VerifyPasswordResetEmailCodeUseCase
    @Inject
    constructor(
        private val repository: PasswordResetRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            verificationCode: String,
        ): PasswordResetResult<PasswordResetVerification> =
            if (verificationCode.length == 6 && verificationCode.all(Char::isDigit)) {
                repository.verifyEmailCode(email.trim().lowercase(Locale.ROOT), verificationCode)
            } else {
                PasswordResetResult.Failure(PasswordResetError.Validation)
            }
    }

class ResetPasswordUseCase
    @Inject
    constructor(
        private val repository: PasswordResetRepository,
    ) {
        suspend operator fun invoke(
            email: String,
            resetToken: String,
            newPassword: String,
            confirmPassword: String,
        ): PasswordResetResult<PasswordResetCompletion> =
            if (newPassword == confirmPassword && ChangeWorkerPasswordUseCase.isValidNewPassword(newPassword)) {
                repository.resetPassword(email.trim().lowercase(Locale.ROOT), resetToken, newPassword, confirmPassword)
            } else {
                PasswordResetResult.Failure(PasswordResetError.Validation)
            }
    }

private fun String.isValidEmail(): Boolean = contains('@') && substringAfter('@').contains('.')
