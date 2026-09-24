package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.dto.UploadResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** 사진 업로드 URL 발급 API다. 명세서 문서의 Method/Path 필드가 다른 API('중앙 답변 수신' 이벤트)와
 * 뒤섞여 있어({@code EVENT eventbridge://...}), 요청/응답 예시와 다른 TEAM_TOKEN 엔드포인트 규칙에
 * 맞춰 {@code POST /api/v1/t/{teamToken}/uploads}로 추정해 구현했다. 원본 Notion 문서 재확인 권고. */
interface UploadApiService {
    @POST("api/v1/t/{teamToken}/uploads")
    suspend fun issueUploadUrls(
        @Path("teamToken") teamToken: String,
        @Body request: UploadRequestDto,
    ): ApiEnvelope<UploadResponseDto>
}
