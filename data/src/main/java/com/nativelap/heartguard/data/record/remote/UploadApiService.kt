package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.dto.UploadResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/** 작업자 세션으로 사진 업로드 URL을 발급한다. */
interface UploadApiService {
    @POST("api/v1/team/uploads")
    suspend fun issueUploadUrls(
        @Body request: UploadRequestDto,
    ): ApiEnvelope<UploadResponseDto>
}
