package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordRequestDto
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.UpdateWorkerProfileRequestDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import javax.inject.Inject

class WorkerProfileRemoteDataSourceImpl
    @Inject
    constructor(
        private val workerProfileApiService: WorkerProfileApiService,
        private val apiExecutor: ApiExecutor,
    ) : WorkerProfileRemoteDataSource {
        /** 로그인한 작업자 계정 정보를 조회한다. 응답 envelope에 data가 없으면 Serialization 계열 실패로 처리된다. */
        override suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto> =
            apiExecutor
                .execute {
                    workerProfileApiService.getWorkerProfile()
                }.requireSuccessData { workerProfile -> workerProfile.userId.isNotBlank() }

        /** 작업자 이름을 수정하고, 서버가 반영한 최신 계정 정보를 돌려준다. */
        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
        ): ApiResult<WorkerProfileResponseDto> =
            apiExecutor
                .execute {
                    workerProfileApiService.updateWorkerProfile(
                        UpdateWorkerProfileRequestDto(
                            name = name,
                            version = version,
                        ),
                    )
                }.requireSuccessData { workerProfile -> workerProfile.userId.isNotBlank() }

        /** 현재 비밀번호를 확인한 뒤 새 비밀번호로 바꾼다. 비밀번호 확인 요청 태그를 붙여 401이 세션 만료로 처리되지 않게 한다. */
        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): ApiResult<ChangeWorkerPasswordResponseDto> =
            apiExecutor
                .execute {
                    val envelope =
                        workerProfileApiService.changePassword(
                            request =
                                ChangeWorkerPasswordRequestDto(
                                    currentPassword = currentPassword,
                                    newPassword = newPassword,
                                ),
                            passwordConfirmationRequest = PasswordConfirmationRequest,
                        )
                    if (envelope.success && envelope.error == null && envelope.data == null) {
                        envelope.copy(data = ChangeWorkerPasswordResponseDto())
                    } else {
                        envelope
                    }
                }.requireSuccessData()
    }
