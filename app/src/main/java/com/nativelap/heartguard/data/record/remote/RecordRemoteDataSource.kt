package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.dto.UploadSlotDto
import okhttp3.RequestBody

interface RecordRemoteDataSource {
    suspend fun issueUploadUrls(files: List<UploadFileRequestDto>): ApiResult<List<UploadSlotDto>>

    suspend fun uploadToPresignedUrl(
        uploadUrl: String,
        requiredHeaders: Map<String, String>,
        photoContent: RequestBody,
    ): ApiResult<Unit>

    suspend fun submitRecord(request: RecordRequestDto, expectedSessionGeneration: Long): ApiResult<RecordResponseDto>
}
