package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginRequestDto
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginResponseDto
import com.nativelap.heartguard.data.auth.dto.AppAccountRegistrationDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.dto.AppEmailCodeVerificationDto
import com.nativelap.heartguard.data.auth.dto.RegisterAppAccountDto
import com.nativelap.heartguard.data.auth.dto.RequestAppEmailCodeDto
import com.nativelap.heartguard.data.auth.dto.VerifyAppEmailCodeDto
import javax.inject.Inject

class AppAuthRemoteDataSourceImpl
    @Inject
    constructor(
        private val appAuthApiService: AppAuthApiService,
        private val appAccountSessionApiService: AppAccountSessionApiService,
        private val apiExecutor: ApiExecutor,
    ) : AppAuthRemoteDataSource {
        override suspend fun requestEmailCode(email: String): ApiResult<AppEmailCodeResponseDto> =
            apiExecutor
                .execute {
                    appAuthApiService.requestEmailCode(RequestAppEmailCodeDto(email))
                }.requireSuccessData { response -> response.expiresAt.isNotBlank() }

        override suspend fun verifyEmailCode(
            email: String,
            verificationCode: String,
        ): ApiResult<AppEmailCodeVerificationDto> =
            apiExecutor
                .execute {
                    appAuthApiService.verifyEmailCode(
                        VerifyAppEmailCodeDto(
                            email = email,
                            verificationCode = verificationCode,
                        ),
                    )
                }.requireSuccessData { response ->
                    response.verified && response.verificationToken.isNotBlank() && response.expiresAt.isNotBlank()
                }

        override suspend fun register(
            email: String,
            password: String,
            confirmPassword: String,
            verificationToken: String,
        ): ApiResult<AppAccountRegistrationDto> =
            apiExecutor
                .execute {
                    appAuthApiService.register(
                        RegisterAppAccountDto(
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            verificationToken = verificationToken,
                        ),
                    )
                }.requireSuccessData { response ->
                    response.userId.isNotBlank() && response.email.isNotBlank()
                }

        override suspend fun login(
            email: String,
            password: String,
        ): ApiResult<AppAccountLoginResponseDto> =
            apiExecutor
                .execute {
                    appAuthApiService.login(AppAccountLoginRequestDto(email = email, password = password))
                }.requireSuccessData { response -> response.accessToken.isNotBlank() }

        override suspend fun logout(): ApiResult<Unit> =
            apiExecutor.execute {
                appAccountSessionApiService.logout()
            }
    }
