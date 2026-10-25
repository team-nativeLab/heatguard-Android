package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginResponseDto
import com.nativelap.heartguard.data.auth.dto.AppAccountRegistrationDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeResponseDto

interface AppAuthRemoteDataSource {
    suspend fun requestEmailCode(email: String): ApiResult<AppEmailCodeResponseDto>

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        verificationCode: String,
    ): ApiResult<AppAccountRegistrationDto>

    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<AppAccountLoginResponseDto>

    suspend fun logout(): ApiResult<Unit>
}
