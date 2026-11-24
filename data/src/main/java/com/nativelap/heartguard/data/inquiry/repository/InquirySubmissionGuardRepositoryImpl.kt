package com.nativelap.heartguard.data.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.inquiry.local.InquirySubmissionGuardLocalDataSource
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionConfirmation
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmissionGuard
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.repository.InquirySubmissionGuardRepository
import javax.inject.Inject

class InquirySubmissionGuardRepositoryImpl
    @Inject
    constructor(
        private val localDataSource: InquirySubmissionGuardLocalDataSource,
    ) : InquirySubmissionGuardRepository {
        override suspend fun read(ownerId: String): ApiResult<InquirySubmissionGuard?> = localDataSource.read(ownerId)

        override suspend fun begin(
            ownerId: String,
            baselineInquiryIds: Set<String>,
            title: String,
            content: String,
        ): ApiResult<InquirySubmissionGuard> = localDataSource.begin(ownerId, baselineInquiryIds, title, content)

        override suspend fun confirmAndClear(
            ownerId: String,
            inquiries: List<InquirySummary>,
            currentTitle: String,
            currentContent: String,
        ): ApiResult<InquirySubmissionConfirmation?> =
            localDataSource.confirmAndClear(ownerId, inquiries, currentTitle, currentContent)

        override suspend fun clear(ownerId: String): ApiResult<Unit> = localDataSource.clear(ownerId)
    }
