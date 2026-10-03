package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import retrofit2.http.Tag
import retrofit2.http.Body
import retrofit2.http.POST

interface RecordApiService {
    @POST("api/v1/team/records")
    suspend fun submitRecord(
        @Body request: RecordRequestDto,
        @Tag owningSession: RequestSessionGeneration? = null,
    ): ApiEnvelope<RecordResponseDto>
}
