package com.nativelap.heartguard.domain.emergency.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus

interface EmergencyCallRepository {
    suspend fun registerEmergencyCall(message: String?): ApiResult<EmergencyCallStatus>

    suspend fun getCurrentEmergencyCallStatus(): ApiResult<EmergencyCallStatus>
}
