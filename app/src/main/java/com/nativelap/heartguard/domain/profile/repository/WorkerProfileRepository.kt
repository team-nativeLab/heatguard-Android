package com.nativelap.heartguard.domain.profile.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile

interface WorkerProfileRepository {
    suspend fun getWorkerProfile(): ApiResult<WorkerProfile>

    suspend fun updateWorkerName(
        name: String,
        version: Long?,
    ): ProfileUpdateResult

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult
}
