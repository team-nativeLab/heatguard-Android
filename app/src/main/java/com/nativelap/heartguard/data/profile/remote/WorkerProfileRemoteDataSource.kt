package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto

interface WorkerProfileRemoteDataSource {
    suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto>

    suspend fun updateWorkerName(name: String): ApiResult<WorkerProfileResponseDto>
}
