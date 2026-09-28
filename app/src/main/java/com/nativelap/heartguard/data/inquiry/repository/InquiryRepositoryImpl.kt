package com.nativelap.heartguard.data.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.inquiry.mapper.toDomain
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import com.nativelap.heartguard.data.inquiry.dto.InquiryListItemDto
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

class InquiryRepositoryImpl @Inject constructor(
    private val inquiryRemoteDataSource: InquiryRemoteDataSource,
) : InquiryRepository {
    override suspend fun submitInquiry(
        title: String,
        content: String,
    ): ApiResult<InquirySubmission> = inquiryRemoteDataSource
        .submitInquiry(title, content)
        .map { response -> response.toDomain() }

    /** hasMore가 false가 될 때까지 다음 커서로 이어 받는다. 서버가 같은 커서를 반복해도 무한 요청하지 않도록 페이지 수를 제한한다. */
    override suspend fun getInquiries(): ApiResult<List<InquirySummary>> {
        val collectedItems = mutableListOf<InquiryListItemDto>()
        var nextCursor: String? = null
        var requestedPageCount = 0

        do {
            val pageResult = inquiryRemoteDataSource.getInquiryPage(
                cursor = nextCursor,
                limit = PAGE_SIZE,
            )
            val inquiryPage = when (pageResult) {
                is ApiResult.Success -> pageResult.value
                is ApiResult.Failure -> return pageResult
            }
            collectedItems += inquiryPage.items
            requestedPageCount += 1

            val receivedCursor = inquiryPage.page?.nextCursor
            val canRequestNextPage = inquiryPage.page?.hasMore == true &&
                receivedCursor != null &&
                receivedCursor != nextCursor &&
                requestedPageCount < MAX_PAGE_COUNT
            nextCursor = receivedCursor
        } while (canRequestNextPage)

        return ApiResult.Success(
            collectedItems.map { inquiryItem -> inquiryItem.toDomain() },
        )
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PAGE_COUNT = 10
    }
}
