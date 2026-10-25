package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.auth.dto.TeamLoginRequestDto
import com.nativelap.heartguard.data.auth.dto.TeamLoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface TeamLoginApiService {
    @POST("api/v1/auth/team/login")
    suspend fun login(
        @Body request: TeamLoginRequestDto,
    ): ApiEnvelope<TeamLoginResponseDto>
}
