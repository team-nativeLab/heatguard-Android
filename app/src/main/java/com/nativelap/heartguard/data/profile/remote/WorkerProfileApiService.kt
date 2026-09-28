package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.profile.dto.UpdateWorkerProfileRequestDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface WorkerProfileApiService {
    @GET("api/v1/auth/team/me")
    suspend fun getWorkerProfile(): ApiEnvelope<WorkerProfileResponseDto>

    @PATCH("api/v1/auth/team/me")
    suspend fun updateWorkerProfile(
        @Body request: UpdateWorkerProfileRequestDto,
    ): ApiEnvelope<WorkerProfileResponseDto>
}
