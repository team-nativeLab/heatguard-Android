package com.nativelap.heartguard.domain.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission

interface InquiryRepository {
    suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmission>
}
