package com.nativelap.heartguard.domain.auth.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.model.AppEmailCodeVerification

interface AppAuthRepository {
    suspend fun requestEmailCode(email: String): ApiResult<AppEmailCode>

    suspend fun verifyEmailCode(
        email: String,
        verificationCode: String,
    ): ApiResult<AppEmailCodeVerification>

    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        verificationToken: String,
    ): ApiResult<AppAccountRegistration>

    suspend fun login(
        email: String,
        password: String,
    ): ApiResult<Unit>

    suspend fun logout(): ApiResult<Unit>
}
