package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.TeamTokenProvider
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.dto.UploadSlotDto
import javax.inject.Inject

class RecordRemoteDataSourceImpl @Inject constructor(
    private val uploadApiService: UploadApiService,
    private val recordApiService: RecordApiService,
    private val externalUploadService: ExternalUploadService,
    private val teamTokenProvider: TeamTokenProvider,
    private val apiExecutor: ApiExecutor,
) : RecordRemoteDataSource {

    override suspend fun issueUploadUrls(files: List<UploadFileRequestDto>): ApiResult<List<UploadSlotDto>> =
        apiExecutor.execute {
            val envelope = uploadApiService.issueUploadUrls(
                teamToken = teamTokenProvider.currentTeamToken(),
                request = UploadRequestDto(files),
            )
            envelope.data?.uploads ?: error("사진 업로드 URL 응답에 data가 없습니다.")
        }

    override suspend fun uploadToPresignedUrl(
        uploadUrl: String,
        requiredHeaders: Map<String, String>,
        contentType: String,
        bytes: ByteArray,
    ): ApiResult<Unit> = apiExecutor.execute {
        externalUploadService.uploadFile(
            uploadUrl = uploadUrl,
            requiredHeaders = requiredHeaders,
            contentType = contentType,
            bytes = bytes,
        )
    }

    override suspend fun submitRecord(request: RecordRequestDto): ApiResult<RecordResponseDto> = apiExecutor.execute {
        val envelope = recordApiService.submitRecord(
            teamToken = teamTokenProvider.currentTeamToken(),
            request = request,
        )
        envelope.data ?: error("현장 기록 등록 응답에 data가 없습니다.")
    }
}
