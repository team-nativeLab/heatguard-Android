package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import java.time.Instant

interface EmergencyCallRemoteDataSource {
    suspend fun registerEmergencyCall(
        idempotencyKey: String,
        clientOccurredAt: Instant,
        message: String?,
    ): ApiResult<EmergencyCallResponseDto>

    suspend fun getCurrentEmergencyCall(): ApiResult<EmergencyCallResponseDto>

    suspend fun updateEmergencyCallStatus(
        callId: String,
        status: EmergencyCallUpdateStatus,
    ): ApiResult<EmergencyCallResponseDto>
}
