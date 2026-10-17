package com.nativelap.heartguard.domain.inquiry.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

/** 문의 화면의 등록 이벤트를 Repository에 전달한다. */
class SubmitInquiryUseCase
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
    ) {
        suspend operator fun invoke(
            title: String,
            content: String,
        ): ApiResult<InquirySubmission> = inquiryRepository.submitInquiry(title, content)
    }
