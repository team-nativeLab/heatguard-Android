package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.inquiry.dto.InquiryListPageDto
import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import javax.inject.Inject

class InquiryRemoteDataSourceImpl @Inject constructor(
    private val inquiryApiService: InquiryApiService,
    private val apiExecutor: ApiExecutor,
) : InquiryRemoteDataSource {
    override suspend fun submitInquiry(
        title: String,
        content: String,
    ): ApiResult<InquirySubmissionResponseDto> = apiExecutor.execute {
        inquiryApiService.submitInquiry(
            SubmitInquiryRequestDto(
                title = title,
                content = content,
            ),
        )
    }.requireSuccessData()

    /** 문의 목록 한 페이지를 조회한다. 첫 페이지는 [cursor]를 null로 보낸다. */
    override suspend fun getInquiryPage(
        cursor: String?,
        limit: Int,
    ): ApiResult<InquiryListPageDto> = apiExecutor.execute {
        inquiryApiService.getInquiries(
            cursor = cursor,
            limit = limit,
        )
    }.requireSuccessData()
}
