package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginRequestDto
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginResponseDto
import com.nativelap.heartguard.data.auth.dto.AppAccountRegistrationDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeVerificationDto
import com.nativelap.heartguard.data.auth.dto.RegisterAppAccountDto
import com.nativelap.heartguard.data.auth.dto.RequestAppEmailCodeDto
import com.nativelap.heartguard.data.auth.dto.VerifyAppEmailCodeDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AppAuthApiService {
    @POST("api/v1/auth/team/email-codes")
    suspend fun requestEmailCode(
        @Body request: RequestAppEmailCodeDto,
    ): ApiEnvelope<AppEmailCodeResponseDto>

    @POST("api/v1/auth/team/email-codes/verify")
    suspend fun verifyEmailCode(
        @Body request: VerifyAppEmailCodeDto,
    ): ApiEnvelope<AppEmailCodeVerificationDto>

    @POST("api/v1/auth/team/register")
    suspend fun register(
        @Body request: RegisterAppAccountDto,
    ): ApiEnvelope<AppAccountRegistrationDto>

    @POST("api/v1/auth/team/login")
    suspend fun login(
        @Body request: AppAccountLoginRequestDto,
    ): ApiEnvelope<AppAccountLoginResponseDto>
}
