package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.inquiry.dto.InquiryListPageDto
import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface InquiryApiService {
    @POST("api/v1/team/inquiries")
    suspend fun submitInquiry(
        @Body request: SubmitInquiryRequestDto,
    ): ApiEnvelope<InquirySubmissionResponseDto>

    // 최신순으로 내려온다. 서버는 알 수 없는 cursor에 400 VALIDATION_ERROR를 준다.
    @GET("api/v1/team/inquiries")
    suspend fun getInquiries(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ApiEnvelope<InquiryListPageDto>
}
