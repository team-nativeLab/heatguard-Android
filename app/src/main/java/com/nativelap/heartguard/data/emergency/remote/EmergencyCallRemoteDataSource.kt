package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.emergency.dto.EmergencyCallResponseDto

interface EmergencyCallRemoteDataSource {
    suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallResponseDto>

    suspend fun getCurrentEmergencyCall(): ApiResult<EmergencyCallResponseDto>
}
