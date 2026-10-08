package com.nativelap.heartguard.domain.inquiry.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

/** 문의하기 화면의 "내 문의 목록"을 한 페이지씩 가져온다. 서버 정렬에 기대지 않고 페이지 안을 등록 시각 최신순으로 정렬한다. */
class GetInquiriesUseCase
    @Inject
    constructor(
        private val inquiryRepository: InquiryRepository,
    ) {
        suspend operator fun invoke(cursor: String?): ApiResult<InquiryPage> =
            when (val pageResult = inquiryRepository.getInquiryPage(cursor)) {
                is ApiResult.Success -> {
                    ApiResult.Success(
                        pageResult.value.copy(
                            inquiries =
                                pageResult.value.inquiries.sortedByDescending { inquirySummary ->
                                    inquirySummary.createdAt
                                },
                        ),
                    )
                }

                is ApiResult.Failure -> {
                    pageResult
                }
            }
    }
