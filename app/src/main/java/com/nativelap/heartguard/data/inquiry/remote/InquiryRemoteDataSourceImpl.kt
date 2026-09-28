package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryResponseDto
import javax.inject.Inject

class InquiryRemoteDataSourceImpl @Inject constructor(
    private val inquiryApiService: InquiryApiService,
    private val apiExecutor: ApiExecutor,
) : InquiryRemoteDataSource {
    override suspend fun submitInquiry(
        title: String,
        content: String,
    ): ApiResult<SubmitInquiryResponseDto> = apiExecutor.execute {
        inquiryApiService.submitInquiry(
            SubmitInquiryRequestDto(title = title, content = content),
        ).data ?: error("문의 등록 응답에 data가 없습니다.")
    }
}
