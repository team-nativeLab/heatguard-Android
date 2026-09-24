package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallRequestDto
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyCallApiService {
    @POST("api/v1/t/{teamToken}/emergency-calls")
    suspend fun registerEmergencyCall(
        @Path("teamToken") teamToken: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: EmergencyCallRequestDto,
    ): ApiEnvelope<EmergencyCallResponseDto>

    @GET("api/v1/t/{teamToken}/emergency-calls/current")
    suspend fun getCurrentEmergencyCall(
        @Path("teamToken") teamToken: String,
    ): ApiEnvelope<EmergencyCallResponseDto>
}
