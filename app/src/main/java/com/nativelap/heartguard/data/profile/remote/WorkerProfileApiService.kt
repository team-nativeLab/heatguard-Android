package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordRequestDto
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.UpdateWorkerProfileRequestDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Tag

interface WorkerProfileApiService {
    @GET("api/v1/auth/team/me")
    suspend fun getWorkerProfile(): ApiEnvelope<WorkerProfileResponseDto>

    @PATCH("api/v1/auth/team/me")
    suspend fun updateWorkerProfile(
        @Body request: UpdateWorkerProfileRequestDto,
    ): ApiEnvelope<WorkerProfileResponseDto>

    // 현재 비밀번호가 틀리면 서버가 401 INVALID_CREDENTIALS를 준다. 태그가 없으면 Authenticator가 세션 만료로 보고 로그아웃시킨다.
    @PUT("api/v1/auth/team/password")
    suspend fun changePassword(
        @Body request: ChangeWorkerPasswordRequestDto,
        @Tag passwordConfirmationRequest: PasswordConfirmationRequest,
    ): ApiEnvelope<ChangeWorkerPasswordResponseDto>
}
