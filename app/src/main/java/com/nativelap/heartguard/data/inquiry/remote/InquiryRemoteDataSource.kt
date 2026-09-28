package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.inquiry.dto.InquiryListPageDto
import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto

interface InquiryRemoteDataSource {
    suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmissionResponseDto>

    suspend fun getInquiryPage(
        cursor: String?,
        limit: Int,
    ): ApiResult<InquiryListPageDto>
}
