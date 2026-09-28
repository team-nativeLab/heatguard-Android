package com.nativelap.heartguard.domain.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary

interface InquiryRepository {
    suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmission>

    /** 내 문의 전체를 최신순으로 돌려준다. 여러 페이지면 이어 받는다. */
    suspend fun getInquiries(): ApiResult<List<InquirySummary>>
}
