package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.dto.UploadSlotDto
import okhttp3.RequestBody
import javax.inject.Inject

class RecordRemoteDataSourceImpl
    @Inject
    constructor(
        private val uploadApiService: UploadApiService,
        private val recordApiService: RecordApiService,
        private val externalUploadService: ExternalUploadService,
        private val apiExecutor: ApiExecutor,
    ) : RecordRemoteDataSource {
        override suspend fun issueUploadUrls(files: List<UploadFileRequestDto>): ApiResult<List<UploadSlotDto>> =
            apiExecutor
                .execute {
                    uploadApiService.issueUploadUrls(
                        request = UploadRequestDto(files),
                    )
                }.requireSuccessData()
                .map { uploadResponse -> uploadResponse.uploads }

        /** presigned URL로 사진을 올린다. 2xx가 아니면(만료된 서명 403 등) 네트워크 오류가 아니라 HTTP 오류로 돌려줘
         * 호출부가 원인을 구분할 수 있게 한다. */
        override suspend fun uploadToPresignedUrl(
            uploadUrl: String,
            requiredHeaders: Map<String, String>,
            photoContent: RequestBody,
        ): ApiResult<Unit> {
            val uploadResult =
                apiExecutor.execute {
                    externalUploadService.uploadFile(
                        uploadUrl = uploadUrl,
                        requiredHeaders = requiredHeaders,
                        photoContent = photoContent,
                    )
                }
            return when (uploadResult) {
                is ApiResult.Success -> {
                    if (uploadResult.value in SUCCESS_STATUS_CODES) {
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Failure(ApiError.Http(statusCode = uploadResult.value))
                    }
                }

                is ApiResult.Failure -> {
                    uploadResult
                }
            }
        }

        override suspend fun submitRecord(
            request: RecordRequestDto,
            expectedSessionGeneration: Long,
        ): ApiResult<RecordResponseDto> =
            apiExecutor
                .execute {
                    recordApiService.submitRecord(request, RequestSessionGeneration(expectedSessionGeneration))
                }.requireSuccessData { recordResponse -> recordResponse.recordId.isNotBlank() }

        private companion object {
            val SUCCESS_STATUS_CODES = 200..299
        }
    }
