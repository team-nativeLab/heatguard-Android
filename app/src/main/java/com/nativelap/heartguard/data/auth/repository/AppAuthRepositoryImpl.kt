package com.nativelap.heartguard.data.auth.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.data.auth.mapper.toDomain
import com.nativelap.heartguard.data.auth.remote.AppAuthRemoteDataSource
import com.nativelap.heartguard.domain.auth.model.AppAccountRegistration
import com.nativelap.heartguard.domain.auth.model.AppEmailCode
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class AppAuthRepositoryImpl
    @Inject
    constructor(
        private val appAuthRemoteDataSource: AppAuthRemoteDataSource,
        private val sessionManager: SessionManager,
    ) : AppAuthRepository {
        override suspend fun requestEmailCode(email: String): ApiResult<AppEmailCode> =
            appAuthRemoteDataSource
                .requestEmailCode(email)
                .map { response -> response.toDomain() }

        override suspend fun register(
            email: String,
            password: String,
            confirmPassword: String,
            verificationCode: String,
        ): ApiResult<AppAccountRegistration> =
            appAuthRemoteDataSource
                .register(email, password, confirmPassword, verificationCode)
                .map { response -> response.toDomain() }

        override suspend fun login(
            email: String,
            password: String,
        ): ApiResult<Unit> =
            when (val result = appAuthRemoteDataSource.login(email, password)) {
                is ApiResult.Failure -> {
                    result
                }

                is ApiResult.Success -> {
                    try {
                        sessionManager.onLoginSucceeded(
                            SessionToken(
                                accessToken = result.value.accessToken,
                                role = SessionRole.APP_ACCOUNT,
                            ),
                        )
                        ApiResult.Success(Unit)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        ApiResult.Failure(ApiError.LocalStorage)
                    }
                }
            }

        override suspend fun logout(): ApiResult<Unit> = appAuthRemoteDataSource.logout()
    }
