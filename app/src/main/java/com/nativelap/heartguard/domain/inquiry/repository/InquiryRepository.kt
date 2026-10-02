package com.nativelap.heartguard.domain.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission

interface InquiryRepository {
    suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmission>

    /** 내 문의 목록의 한 페이지를 받는다. 첫 페이지는 [cursor]를 null로 요청한다. */
    suspend fun getInquiryPage(cursor: String?): ApiResult<InquiryPage>
}
