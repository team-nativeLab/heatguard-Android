package com.nativelap.heartguard.data.auth.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.auth.mapper.toDomain
import com.nativelap.heartguard.data.auth.remote.AppAuthRemoteDataSource
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.model.AppEmailCodeVerification
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import javax.inject.Inject

class AppAuthRepositoryImpl
    @Inject
    constructor(
        private val appAuthRemoteDataSource: AppAuthRemoteDataSource,
    ) : AppAuthRepository {
        override suspend fun requestEmailCode(email: String): ApiResult<AppEmailCode> =
            appAuthRemoteDataSource
                .requestEmailCode(email)
                .map { response -> response.toDomain() }

        override suspend fun verifyEmailCode(
            email: String,
            verificationCode: String,
        ): ApiResult<AppEmailCodeVerification> =
            appAuthRemoteDataSource
                .verifyEmailCode(email, verificationCode)
                .map { response -> response.toDomain() }

        override suspend fun register(
            email: String,
            password: String,
            confirmPassword: String,
            verificationToken: String,
        ): ApiResult<AppAccountRegistration> =
            appAuthRemoteDataSource
                .register(email, password, confirmPassword, verificationToken)
                .map { response -> response.toDomain() }

        override suspend fun logout(): ApiResult<Unit> = appAuthRemoteDataSource.logout()
    }
