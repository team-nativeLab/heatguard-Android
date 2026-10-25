package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

interface RecordApiService {
    @POST("api/v1/team/records")
    suspend fun submitRecord(
        @Body request: RecordRequestDto,
        @Tag owningSession: RequestSessionGeneration? = null,
    ): ApiEnvelope<RecordResponseDto>
}
