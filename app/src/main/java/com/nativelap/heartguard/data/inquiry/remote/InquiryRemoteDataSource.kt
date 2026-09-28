package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryResponseDto

interface InquiryRemoteDataSource {
    suspend fun submitInquiry(title: String, content: String): ApiResult<SubmitInquiryResponseDto>
}
