package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordResponseDto
import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import com.nativelap.heartguard.data.profile.dto.ProfilePhotoResponseDto
import com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadRequestDto
import com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadResponseDto

interface WorkerProfileRemoteDataSource {
    suspend fun getWorkerProfile(expectedSessionGeneration: Long? = null): ApiResult<WorkerProfileResponseDto>

    suspend fun updateWorkerName(
        name: String,
        version: Long?,
        expectedSessionGeneration: Long? = null,
    ): ApiResult<WorkerProfileResponseDto>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<ChangeWorkerPasswordResponseDto>

    suspend fun createProfilePhotoUpload(
        request: ProfilePhotoUploadRequestDto,
        expectedSessionGeneration: Long,
    ): ApiResult<ProfilePhotoUploadResponseDto>

    suspend fun confirmProfilePhotoUpload(
        uploadId: String,
        expectedSessionGeneration: Long,
    ): ApiResult<ProfilePhotoResponseDto>

    suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhotoResponseDto>

    suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit>
}
