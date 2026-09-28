package com.nativelap.heartguard.data.inquiry.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.inquiry.mapper.toDomain
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import javax.inject.Inject

class InquiryRepositoryImpl @Inject constructor(
    private val inquiryRemoteDataSource: InquiryRemoteDataSource,
) : InquiryRepository {
    override suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmission> =
        inquiryRemoteDataSource.submitInquiry(title, content).map { it.toDomain() }
}
