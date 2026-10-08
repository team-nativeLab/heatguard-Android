package com.nativelap.heartguard.data.inquiry.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.inquiry.mapper.toDomain
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

class InquiryRepositoryImpl
    @Inject
    constructor(
        private val inquiryRemoteDataSource: InquiryRemoteDataSource,
    ) : InquiryRepository {
        override suspend fun submitInquiry(
            title: String,
            content: String,
        ): ApiResult<InquirySubmission> =
            inquiryRemoteDataSource
                .submitInquiry(title, content)
                .map { response -> response.toDomain() }

        /** 명세상 필수인 page 정보가 없거나, 더 있다는데 다음 커서가 없으면 목록이 잘린 것이므로 실패로 바꾼다. */
        override suspend fun getInquiryPage(cursor: String?): ApiResult<InquiryPage> {
            val pageResult =
                inquiryRemoteDataSource.getInquiryPage(
                    cursor = cursor,
                    limit = PAGE_SIZE,
                )
            val inquiryPage =
                when (pageResult) {
                    is ApiResult.Success -> pageResult.value
                    is ApiResult.Failure -> return pageResult
                }
            val pageInfo = inquiryPage.page ?: return ApiResult.Failure(ApiError.Unknown)
            val receivedCursor = pageInfo.nextCursor?.takeIf(String::isNotBlank)
            if (pageInfo.hasMore && receivedCursor == null) {
                return ApiResult.Failure(ApiError.Unknown)
            }

            return ApiResult.Success(
                InquiryPage(
                    inquiries = inquiryPage.items.map { inquiryItem -> inquiryItem.toDomain() },
                    nextCursor = receivedCursor.takeIf { pageInfo.hasMore },
                ),
            )
        }

        private companion object {
            const val PAGE_SIZE = 50
        }
    }
