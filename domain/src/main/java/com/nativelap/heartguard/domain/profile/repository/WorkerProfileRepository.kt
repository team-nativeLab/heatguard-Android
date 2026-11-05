package com.nativelap.heartguard.domain.profile.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile

interface WorkerProfileRepository {
    suspend fun getWorkerProfile(expectedSessionGeneration: Long? = null): ApiResult<WorkerProfile>

    suspend fun updateWorkerName(
        name: String,
        version: Long?,
        expectedSessionGeneration: Long? = null,
    ): ProfileUpdateResult

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult

    suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?>

    /** 세션 종료 시 보관 중인 프로필 사진 조회 URL을 동기적으로 제거한다. */
    fun clearCachedProfilePhoto(sessionGeneration: Long) = Unit

    suspend fun uploadProfilePhoto(
        photoUri: String,
        expectedSessionGeneration: Long,
    ): ApiResult<ProfilePhoto>

    suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit>
}
