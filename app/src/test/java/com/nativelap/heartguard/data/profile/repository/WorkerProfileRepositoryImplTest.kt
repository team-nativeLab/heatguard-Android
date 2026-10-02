package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
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

    @Test
    fun `이름 저장 결과를 성공·충돌·실패로 바꾸고 버전을 함께 보낸다`() = runTest {
        val conflictResult = updateNameWith(ApiResult.Failure(ApiError.Http(statusCode = 409, errorCode = "CONFLICT")))
        val failureResult = updateNameWith(ApiResult.Failure(ApiError.Network))
        val successResult = updateNameWith(
            ApiResult.Success(
                WorkerProfileResponseDto(
                    userId = "usr_01",
                    name = "김현장",
                    companyName = "이음산업건설",
                    version = 4,
                ),
            ),
        )

        assertEquals(ProfileUpdateResult.Conflict, conflictResult)
        assertEquals(ProfileUpdateResult.Failure, failureResult)
        val savedProfile = (successResult as ProfileUpdateResult.Success).profile
        assertEquals("이음산업건설", savedProfile.companyName)
        assertEquals(4L, savedProfile.version)
        assertEquals(listOf("김현장" to 3L), requestedUpdates)
    }

    private val requestedUpdates = mutableListOf<Pair<String, Long?>>()

    private suspend fun updateNameWith(
        remoteResult: ApiResult<WorkerProfileResponseDto>,
    ): ProfileUpdateResult {
        requestedUpdates.clear()
        val remoteDataSource = object : WorkerProfileRemoteDataSource {
            override suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto> = error("not used")

            override suspend fun updateWorkerName(
                name: String,
                version: Long?,
            ): ApiResult<WorkerProfileResponseDto> {
                requestedUpdates += name to version
                return remoteResult
            }

            override suspend fun changePassword(
                currentPassword: String,
                newPassword: String,
            ): ApiResult<ChangeWorkerPasswordResponseDto> = error("not used")
        }
        return WorkerProfileRepositoryImpl(remoteDataSource).updateWorkerName(
            name = "김현장",
            version = 3,
        )
    }

    private suspend fun changePasswordWith(
        remoteResult: ApiResult<ChangeWorkerPasswordResponseDto>,
    ): PasswordChangeResult {
        val remoteDataSource = object : WorkerProfileRemoteDataSource {
            override suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto> = error("not used")

            override suspend fun updateWorkerName(
                name: String,
                version: Long?,
            ): ApiResult<WorkerProfileResponseDto> = error("not used")

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
