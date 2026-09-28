package com.nativelap.heartguard.domain.profile.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile

interface WorkerProfileRepository {
    suspend fun getWorkerProfile(): ApiResult<WorkerProfile>

    suspend fun updateWorkerName(name: String): ApiResult<WorkerProfile>
}
