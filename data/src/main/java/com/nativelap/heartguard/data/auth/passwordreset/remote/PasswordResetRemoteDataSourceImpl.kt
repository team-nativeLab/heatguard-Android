package com.nativelap.heartguard.data.auth.passwordreset.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetCompletionResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetVerificationResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.RequestPasswordResetEmailCodeDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.ResetPasswordDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.VerifyPasswordResetEmailCodeDto
import java.time.Instant
import javax.inject.Inject

class PasswordResetRemoteDataSourceImpl
    @Inject
    constructor(
        private val service: PasswordResetApiService,
        private val executor: ApiExecutor,
    ) : PasswordResetRemoteDataSource {
        override suspend fun requestEmailCode(email: String) =
            executor
                .execute { service.requestEmailCode(RequestPasswordResetEmailCodeDto(email)) }
                .requireSuccessData { response -> response.expiresAt.isValidExpiration() }

        override suspend fun verifyEmailCode(
            email: String,
            code: String,
        ) = executor
            .execute { service.verifyEmailCode(VerifyPasswordResetEmailCodeDto(email, code)) }
            .requireSuccessData { response ->
                response.resetToken.isNotBlank() && response.expiresAt.isValidExpiration()
            }

        override suspend fun resetPassword(
            email: String,
            token: String,
            password: String,
            confirmation: String,
        ) = executor
            .execute { service.resetPassword(ResetPasswordDto(email, token, password, confirmation)) }
            .requireSuccessData { response -> response.passwordChanged && response.changedAt.isNotBlank() }
    }

private fun String.isValidExpiration(): Boolean = runCatching { Instant.parse(this) }.isSuccess
