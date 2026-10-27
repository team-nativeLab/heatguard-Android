package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginResponseDto
import com.nativelap.heartguard.data.auth.dto.AppAccountRegistrationDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeVerificationDto

interface AppAuthRemoteDataSource {
    suspend fun requestEmailCode(email: String): ApiResult<AppEmailCodeResponseDto>

    suspend fun verifyEmailCode(
        email: String,
        verificationCode: String,
    ): ApiResult<AppEmailCodeVerificationDto>

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        verificationToken: String,
    ): ApiResult<AppAccountRegistrationDto>

    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<AppAccountLoginResponseDto>

    suspend fun logout(): ApiResult<Unit>
}
