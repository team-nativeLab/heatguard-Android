package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface InquiryApiService {
    @POST("api/v1/team/inquiries")
    suspend fun submitInquiry(
        @Body request: SubmitInquiryRequestDto,
    ): ApiEnvelope<SubmitInquiryResponseDto>
}
