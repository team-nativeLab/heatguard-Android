package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto

interface WorkerProfileRemoteDataSource {
    suspend fun getWorkerProfile(): ApiResult<WorkerProfileResponseDto>

    suspend fun updateWorkerName(
        name: String,
        version: Long?,
    ): ApiResult<WorkerProfileResponseDto>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<ChangeWorkerPasswordResponseDto>
}
