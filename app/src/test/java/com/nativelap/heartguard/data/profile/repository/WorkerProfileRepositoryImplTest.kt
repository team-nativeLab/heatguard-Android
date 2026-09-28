package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkerProfileRepositoryImplTest {

    @Test
    fun `비밀번호 변경 오류 코드를 도메인 결과로 바꾼다`() = runTest {
        assertEquals(
            PasswordChangeResult.InvalidCurrentPassword,
            changePasswordWith(ApiResult.Failure(ApiError.Http(statusCode = 401, errorCode = "INVALID_CREDENTIALS"))),
        )
        assertEquals(
            PasswordChangeResult.InvalidNewPassword,
            changePasswordWith(ApiResult.Failure(ApiError.Http(statusCode = 400, errorCode = "VALIDATION_ERROR"))),
        )
        assertEquals(
            PasswordChangeResult.Failure,
            changePasswordWith(ApiResult.Failure(ApiError.Network)),
        )
        assertEquals(
            PasswordChangeResult.Success,
            changePasswordWith(ApiResult.Success(ChangeWorkerPasswordResponseDto(changedAt = "2026-09-28T14:35:53+00:00"))),
        )
    }

    private suspend fun changePasswordWith(
        remoteResult: ApiResult<ChangeWorkerPasswordResponseDto>,
    ): PasswordChangeResult {
        val remoteDataSource = object : WorkerProfileRemoteDataSource {
            override suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto> = error("not used")

            override suspend fun updateWorkerName(name: String): ApiResult<WorkerProfileResponseDto> = error("not used")

            override suspend fun changePassword(
                currentPassword: String,
                newPassword: String,
            ): ApiResult<ChangeWorkerPasswordResponseDto> = remoteResult
        }
        return WorkerProfileRepositoryImpl(remoteDataSource).changePassword(
            currentPassword = "current1",
            newPassword = "abcd1234",
        )
    }
}
