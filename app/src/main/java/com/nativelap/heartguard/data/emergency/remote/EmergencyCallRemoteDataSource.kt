package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus

interface EmergencyCallRemoteDataSource {
    suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallResponseDto>

    suspend fun getCurrentEmergencyCall(): ApiResult<EmergencyCallResponseDto>

    suspend fun updateEmergencyCallStatus(
        callId: String,
        status: EmergencyCallUpdateStatus,
    ): ApiResult<EmergencyCallResponseDto>
}
