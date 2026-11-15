package com.nativelap.heartguard.data.auth.passwordreset.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetCompletionResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetVerificationResponseDto

interface PasswordResetRemoteDataSource {
    suspend fun requestEmailCode(email: String): ApiResult<PasswordResetEmailCodeResponseDto>

    suspend fun verifyEmailCode(
        email: String,
        code: String,
    ): ApiResult<PasswordResetVerificationResponseDto>

    suspend fun resetPassword(
        email: String,
        token: String,
        password: String,
        confirmation: String,
    ): ApiResult<PasswordResetCompletionResponseDto>
}
