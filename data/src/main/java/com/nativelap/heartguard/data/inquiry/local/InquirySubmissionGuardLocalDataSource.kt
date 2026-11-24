package com.nativelap.heartguard.data.inquiry.local

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionConfirmation
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionGuard
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary

interface InquirySubmissionGuardLocalDataSource {
    suspend fun read(ownerId: String): ApiResult<InquirySubmissionGuard?>

    suspend fun begin(
        ownerId: String,
        baselineInquiryIds: Set<String>,
        title: String,
        content: String,
    ): ApiResult<InquirySubmissionGuard>

    suspend fun confirmAndClear(
        ownerId: String,
        inquiries: List<InquirySummary>,
        currentTitle: String,
        currentContent: String,
    ): ApiResult<InquirySubmissionConfirmation?>

    suspend fun clear(ownerId: String): ApiResult<Unit>
}
