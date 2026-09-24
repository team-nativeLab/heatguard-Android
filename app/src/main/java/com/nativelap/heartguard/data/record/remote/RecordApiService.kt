package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface RecordApiService {
    @POST("api/v1/t/{teamToken}/records")
    suspend fun submitRecord(
        @Path("teamToken") teamToken: String,
        @Body request: RecordRequestDto,
    ): ApiEnvelope<RecordResponseDto>
}
