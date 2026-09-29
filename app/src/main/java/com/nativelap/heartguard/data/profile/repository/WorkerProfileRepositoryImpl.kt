package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.profile.mapper.toDomain
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

class WorkerProfileRepositoryImpl @Inject constructor(
    private val workerProfileRemoteDataSource: WorkerProfileRemoteDataSource,
) : WorkerProfileRepository {
    override suspend fun getWorkerProfile(): ApiResult<WorkerProfile> = workerProfileRemoteDataSource
        .getWorkerProfile()
        .map { profileResponse -> profileResponse.toDomain() }

    /** 이름을 저장하고, 409(다른 곳에서 먼저 수정됨)는 충돌 결과로 바꾼다. */
    override suspend fun updateWorkerName(
        name: String,
        version: Long?,
    ): ProfileUpdateResult {
        val updateResult = workerProfileRemoteDataSource.updateWorkerName(
            name = name,
            version = version,
        )
        return when (updateResult) {
            is ApiResult.Success -> ProfileUpdateResult.Success(updateResult.value.toDomain())
            is ApiResult.Failure -> updateResult.error.toProfileUpdateFailure()
        }
    }

    /** 서버 오류 코드를 화면이 분기할 비밀번호 변경 결과로 바꾼다. */
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult {
        val changeResult = workerProfileRemoteDataSource.changePassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
        )
        return when (changeResult) {
            is ApiResult.Success -> PasswordChangeResult.Success
            is ApiResult.Failure -> changeResult.error.toPasswordChangeFailure()
        }
    }

    private fun ApiError.toProfileUpdateFailure(): ProfileUpdateResult = when {
        this is ApiError.Http && statusCode == HTTP_CONFLICT -> ProfileUpdateResult.Conflict
        else -> ProfileUpdateResult.Failure
    }

    private fun ApiError.toPasswordChangeFailure(): PasswordChangeResult = when (this) {
        is ApiError.Http -> when {
            statusCode == HTTP_UNAUTHORIZED && errorCode == INVALID_CREDENTIALS_CODE -> {
                PasswordChangeResult.InvalidCurrentPassword
            }

            statusCode == HTTP_BAD_REQUEST && errorCode == VALIDATION_ERROR_CODE -> {
                PasswordChangeResult.InvalidNewPassword
            }

            else -> PasswordChangeResult.Failure
        }

        ApiError.Network,
        ApiError.Serialization,
        ApiError.Unknown,
        -> PasswordChangeResult.Failure
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
        const val INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS"
        const val VALIDATION_ERROR_CODE = "VALIDATION_ERROR"
    }
}
