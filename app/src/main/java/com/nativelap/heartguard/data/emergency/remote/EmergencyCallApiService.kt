package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallRequestDto
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.data.emergency.dto.UpdateEmergencyCallStatusRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface EmergencyCallApiService {
    @POST("api/v1/team/emergency-calls")
    suspend fun registerEmergencyCall(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: EmergencyCallRequestDto,
    ): ApiEnvelope<EmergencyCallResponseDto>

    @GET("api/v1/team/emergency-calls/current")
    suspend fun getCurrentEmergencyCall(): ApiEnvelope<EmergencyCallResponseDto>

    @PATCH("api/v1/team/emergency-calls/{callId}")
    suspend fun updateEmergencyCallStatus(
        @Path("callId") callId: String,
        @Body request: UpdateEmergencyCallStatusRequestDto,
    ): ApiEnvelope<EmergencyCallResponseDto>
}
