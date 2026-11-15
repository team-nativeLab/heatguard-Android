package com.nativelap.heartguard.data.auth.passwordreset.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetCompletionResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetVerificationResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.RequestPasswordResetEmailCodeDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.ResetPasswordDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.VerifyPasswordResetEmailCodeDto
import retrofit2.http.Body
import retrofit2.http.POST

interface PasswordResetApiService {
    @POST("api/v1/auth/password-reset/email-codes")
    suspend fun requestEmailCode(
        @Body body: RequestPasswordResetEmailCodeDto,
    ): ApiEnvelope<PasswordResetEmailCodeResponseDto>

    @POST("api/v1/auth/password-reset/email-codes/verify")
    suspend fun verifyEmailCode(
        @Body body: VerifyPasswordResetEmailCodeDto,
    ): ApiEnvelope<PasswordResetVerificationResponseDto>

    @POST("api/v1/auth/password-reset")
    suspend fun resetPassword(
        @Body body: ResetPasswordDto,
    ): ApiEnvelope<PasswordResetCompletionResponseDto>
}
