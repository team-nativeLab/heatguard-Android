package com.nativelap.heartguard.domain.inquiry.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionConfirmation
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionGuard
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.repository.InquirySubmissionGuardRepository
import javax.inject.Inject

class InquirySubmissionGuardUseCase
    @Inject
    constructor(
        private val inquirySubmissionGuardRepository: InquirySubmissionGuardRepository,
    ) {
        suspend fun restore(ownerId: String): ApiResult<InquirySubmissionGuard?> =
            inquirySubmissionGuardRepository.read(ownerId)

        suspend fun begin(
            ownerId: String,
            baselineInquiryIds: Set<String>,
            title: String,
            content: String,
        ): ApiResult<InquirySubmissionGuard> =
            inquirySubmissionGuardRepository.begin(ownerId, baselineInquiryIds, title, content)

        suspend fun confirmAndClear(
            ownerId: String,
            inquiries: List<InquirySummary>,
            currentTitle: String,
            currentContent: String,
        ): ApiResult<InquirySubmissionConfirmation?> =
            inquirySubmissionGuardRepository.confirmAndClear(
                ownerId,
                inquiries,
                currentTitle,
                currentContent,
            )

        suspend fun clear(ownerId: String): ApiResult<Unit> = inquirySubmissionGuardRepository.clear(ownerId)
    }
