package com.nativelap.heartguard.data.profile.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.data.profile.ProfilePhotoFileReader
import com.nativelap.heartguard.data.profile.mapper.toDomain as profilePhotoToDomain
import com.nativelap.heartguard.data.profile.mapper.toDomain
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadRequestDto
import com.nativelap.heartguard.data.record.remote.ExternalUploadService
import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto
import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import com.nativelap.heartguard.core.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.withContext

class WorkerProfileRepositoryImpl
    @Inject
    constructor(
        private val workerProfileRemoteDataSource: WorkerProfileRemoteDataSource,
        private val profilePhotoFileReader: ProfilePhotoFileReader,
        private val externalUploadService: ExternalUploadService,
        private val sessionManager: SessionManager,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : WorkerProfileRepository {
        override suspend fun getWorkerProfile(expectedSessionGeneration: Long?): ApiResult<WorkerProfile> =
            workerProfileRemoteDataSource
                .getWorkerProfile(expectedSessionGeneration)
                .map { profileResponse -> profileResponse.toDomain() }

        /** 이름을 저장하고, 409(다른 곳에서 먼저 수정됨)는 충돌 결과로 바꾼다. */
        override suspend fun updateWorkerName(
            name: String,
            version: Long?,
            expectedSessionGeneration: Long?,
        ): ProfileUpdateResult {
            val updateResult =
                workerProfileRemoteDataSource.updateWorkerName(
                    name = name,
                    version = version,
                    expectedSessionGeneration = expectedSessionGeneration,
                )
            return when (updateResult) {
                is ApiResult.Success -> ProfileUpdateResult.Success(updateResult.value.toDomain())
                is ApiResult.Failure -> updateResult.error.toProfileUpdateFailure()
            }
        }

        /** 서버 오류 코드를 화면이 분기할 비밀번호 변경 결과로 바꾼다. */
        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult {
            val changeResult =
                workerProfileRemoteDataSource.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                )
            return when (changeResult) {
                is ApiResult.Success -> PasswordChangeResult.Success
                is ApiResult.Failure -> changeResult.error.toPasswordChangeFailure()
            }
        }

        override suspend fun getProfilePhoto(expectedSessionGeneration: Long): ApiResult<ProfilePhoto?> =
            workerProfileRemoteDataSource
                .getProfilePhoto(expectedSessionGeneration)
                .map { photoResponse ->
                    photoResponse.profilePhotoToDomain().takeIf { photo -> !photo.photoUrl.isNullOrBlank() }
                }

        override suspend fun uploadProfilePhoto(
            photoUri: String,
            expectedSessionGeneration: Long,
        ): ApiResult<ProfilePhoto> {
            val uploadPayload =
                try {
                    withContext(ioDispatcher) {
                        profilePhotoFileReader.read(Uri.parse(photoUri))
                    }
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    return ApiResult.Failure(ApiError.LocalStorage)
                }

            val uploadRequest =
                ProfilePhotoUploadRequestDto(
                    contentType = uploadPayload.contentType,
                    size = uploadPayload.sizeBytes,
                )
            if (!isCurrentTeamSession(expectedSessionGeneration)) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            val uploadLinkResult =
                workerProfileRemoteDataSource.createProfilePhotoUpload(
                    request = uploadRequest,
                    expectedSessionGeneration = expectedSessionGeneration,
                )
            if (uploadLinkResult is ApiResult.Failure) {
                return uploadLinkResult
            }
            val uploadLink = (uploadLinkResult as ApiResult.Success).value
            if (!uploadLink.isUsableAt(Instant.now())) {
                return ApiResult.Failure(ApiError.Http(statusCode = HTTP_GONE))
            }
            if (!isCurrentTeamSession(expectedSessionGeneration)) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }

            val uploadStatus =
                try {
                    externalUploadService.uploadFile(
                        uploadUrl = uploadLink.uploadUrl,
                        requiredHeaders = uploadLink.requiredHeaders,
                        photoContent = profilePhotoFileReader.createRequestBody(uploadPayload),
                        allowRedirects = false,
                    )
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: IOException) {
                    return ApiResult.Failure(ApiError.Network)
                } catch (_: Exception) {
                    return ApiResult.Failure(ApiError.Unknown)
                }
            if (uploadStatus !in SUCCESS_STATUS_CODES) {
                return ApiResult.Failure(ApiError.Http(statusCode = uploadStatus))
            }
            if (!isCurrentTeamSession(expectedSessionGeneration)) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }

            return workerProfileRemoteDataSource
                .confirmProfilePhotoUpload(
                    uploadId = uploadLink.uploadId,
                    expectedSessionGeneration = expectedSessionGeneration,
                )
                .map { photoResponse -> photoResponse.profilePhotoToDomain() }
        }

        override suspend fun deleteProfilePhoto(expectedSessionGeneration: Long): ApiResult<Unit> {
            if (!isCurrentTeamSession(expectedSessionGeneration)) {
                return ApiResult.Failure(ApiError.SessionChanged)
            }
            return workerProfileRemoteDataSource.deleteProfilePhoto(expectedSessionGeneration)
        }

        private fun isCurrentTeamSession(expectedSessionGeneration: Long): Boolean {
            val currentSession = sessionManager.getSnapshot()
            return currentSession.generation == expectedSessionGeneration &&
                currentSession.role == SessionRole.TEAM_MEMBER &&
                !currentSession.accessToken.isNullOrBlank()
        }

        private fun com.nativelap.heartguard.data.profile.dto.ProfilePhotoUploadResponseDto.isUsableAt(
            now: Instant,
        ): Boolean {
            val uploadUri = Uri.parse(uploadUrl)
            if (uploadUri.scheme != HTTPS_SCHEME || uploadUri.host.isNullOrBlank()) {
                return false
            }
            return try {
                OffsetDateTime.parse(expiresAt).toInstant().isAfter(now)
            } catch (_: Exception) {
                false
            }
        }

        private fun ApiError.toProfileUpdateFailure(): ProfileUpdateResult =
            when {
                this is ApiError.Http && statusCode == HTTP_CONFLICT -> ProfileUpdateResult.Conflict
                else -> ProfileUpdateResult.Failure
            }

        private fun ApiError.toPasswordChangeFailure(): PasswordChangeResult =
            when (this) {
                is ApiError.Http -> {
                    when {
                        statusCode == HTTP_UNAUTHORIZED && errorCode == INVALID_CREDENTIALS_CODE -> {
                            PasswordChangeResult.InvalidCurrentPassword
                        }

                        statusCode == HTTP_BAD_REQUEST && errorCode == VALIDATION_ERROR_CODE -> {
                            PasswordChangeResult.InvalidNewPassword
                        }

                        else -> {
                            PasswordChangeResult.Failure
                        }
                    }
                }

                ApiError.Network,
                ApiError.SessionChanged,
                ApiError.SiteNotSelected,
                ApiError.Serialization,
                ApiError.Unknown,
                ApiError.LocalStorage,
                is ApiError.ServerRejected,
                -> {
                    PasswordChangeResult.Failure
                }
            }

        private companion object {
            const val HTTPS_SCHEME = "https"
            const val HTTP_GONE = 410
            val SUCCESS_STATUS_CODES = 200..299
            const val HTTP_BAD_REQUEST = 400
            const val HTTP_UNAUTHORIZED = 401
            const val HTTP_CONFLICT = 409
            const val INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS"
            const val VALIDATION_ERROR_CODE = "VALIDATION_ERROR"
        }
    }
