package com.nativelap.heartguard.data.auth.passwordreset.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetRemoteDataSource
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetCompletion
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetEmailCode
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetVerification
import com.nativelap.heartguard.domain.auth.passwordreset.repository.PasswordResetRepository
import javax.inject.Inject

class PasswordResetRepositoryImpl
    @Inject
    constructor(
        private val remote: PasswordResetRemoteDataSource,
    ) : PasswordResetRepository {
        override suspend fun requestEmailCode(email: String) =
            remote.requestEmailCode(email).mapResult { PasswordResetEmailCode(it.expiresAt) }

        override suspend fun verifyEmailCode(
            email: String,
            verificationCode: String,
        ) = remote.verifyEmailCode(email, verificationCode).mapResult {
            PasswordResetVerification(it.expiresAt, it.resetToken)
        }

        override suspend fun resetPassword(
            email: String,
            resetToken: String,
            newPassword: String,
            confirmPassword: String,
        ) = remote.resetPassword(email, resetToken, newPassword, confirmPassword).mapResult {
            PasswordResetCompletion(it.changedAt)
        }

        private fun <Dto, Model> ApiResult<Dto>.mapResult(transform: (Dto) -> Model): PasswordResetResult<Model> =
            when (this) {
                is ApiResult.Success -> PasswordResetResult.Success(transform(value))
                is ApiResult.Failure -> PasswordResetResult.Failure(error.toPasswordResetError())
            }

        private fun ApiError.toPasswordResetError(): PasswordResetError =
            when (this) {
                is ApiError.ServerRejected -> {
                    when (errorCode) {
                        "INVALID_VERIFICATION_CODE" -> PasswordResetError.InvalidCode
                        "INVALID_RESET_TOKEN" -> PasswordResetError.InvalidToken
                        "VALIDATION_ERROR" -> PasswordResetError.Validation
                        else -> PasswordResetError.Generic
                    }
                }

                is ApiError.Http -> {
                    when {
                        statusCode == 429 || errorCode == "RATE_LIMITED" -> PasswordResetError.RateLimited
                        statusCode == 503 || errorCode == "SERVICE_DEGRADED" -> PasswordResetError.ServiceUnavailable
                        errorCode == "INVALID_VERIFICATION_CODE" -> PasswordResetError.InvalidCode
                        errorCode == "INVALID_RESET_TOKEN" -> PasswordResetError.InvalidToken
                        errorCode == "VALIDATION_ERROR" -> PasswordResetError.Validation
                        else -> PasswordResetError.Generic
                    }
                }

                ApiError.Network -> {
                    PasswordResetError.Network
                }

                else -> {
                    PasswordResetError.Generic
                }
            }
    }
