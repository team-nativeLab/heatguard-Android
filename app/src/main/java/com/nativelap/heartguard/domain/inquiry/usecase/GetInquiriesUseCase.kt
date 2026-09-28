package com.nativelap.heartguard.domain.inquiry.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

/** 문의하기 화면의 "내 문의 목록"을 최신 등록순으로 가져온다. 서버 정렬에 기대지 않고 등록 시각으로 다시 정렬한다. */
class GetInquiriesUseCase @Inject constructor(
    private val inquiryRepository: InquiryRepository,
) {
    suspend operator fun invoke(): ApiResult<List<InquirySummary>> {
        return when (val inquiriesResult = inquiryRepository.getInquiries()) {
            is ApiResult.Success -> ApiResult.Success(
                inquiriesResult.value.sortedByDescending { inquirySummary -> inquirySummary.createdAt },
            )

            is ApiResult.Failure -> inquiriesResult
        }
    }
}
