package com.nativelap.heartguard.data.inquiry.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.inquiry.dto.InquiryListCursorDto
import com.nativelap.heartguard.data.inquiry.dto.InquiryListItemDto
import com.nativelap.heartguard.data.inquiry.dto.InquiryListPageDto
import com.nativelap.heartguard.data.inquiry.dto.InquirySubmissionResponseDto
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InquiryRepositoryImplTest {
    @Test
    fun returnsOnePageWithNextCursorWhenMoreInquiriesRemain() = runTest {
        val source = FakeInquirySource(mapOf(null to page(0, "cur_1", true)))
        val inquiryResult = InquiryRepositoryImpl(source).getInquiryPage(cursor = null)
        val inquiryPage = (inquiryResult as ApiResult.Success).value
        assertEquals(listOf("inq_0"), inquiryPage.inquiries.map { inquirySummary -> inquirySummary.inquiryId })
        assertEquals("cur_1", inquiryPage.nextCursor)
        assertEquals(listOf(null), source.requestedCursors)
    }

    @Test
    fun lastPageHasNoNextCursorEvenIfServerSendsOne() = runTest {
        val source = FakeInquirySource(mapOf("cur_1" to page(1, "stale_cursor", false)))
        val inquiryResult = InquiryRepositoryImpl(source).getInquiryPage(cursor = "cur_1")
        assertEquals(null, (inquiryResult as ApiResult.Success).value.nextCursor)
    }

    @Test
    fun missingOrBlankContinuationCursorIsFailure() = runTest {
        listOf(null, "", " ").forEach { cursor ->
            val source = FakeInquirySource(mapOf(null to page(0, cursor, true)))
            assertEquals(ApiResult.Failure(ApiError.Unknown), InquiryRepositoryImpl(source).getInquiryPage(null))
        }
    }

    @Test
    fun pageWithoutPaginationInfoIsFailure() = runTest {
        val source = FakeInquirySource(mapOf(null to InquiryListPageDto(items = listOf(InquiryListItemDto("inq_0")))))
        assertEquals(ApiResult.Failure(ApiError.Unknown), InquiryRepositoryImpl(source).getInquiryPage(null))
    }

    @Test
    fun requestFailureIsPreserved() = runTest {
        val source = FakeInquirySource(emptyMap())
        assertEquals(ApiResult.Failure(ApiError.Network), InquiryRepositoryImpl(source).getInquiryPage("missing"))
    }

    @Test
    fun cancellationIsPropagated() = runTest {
        val source = FakeInquirySource(emptyMap(), cancelRequests = true)
        try {
            InquiryRepositoryImpl(source).getInquiryPage(null)
            throw AssertionError("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, source.requestedCursors.size)
        }
    }

    private fun page(index: Int, cursor: String?, hasMore: Boolean): InquiryListPageDto {
        return InquiryListPageDto(
            items = listOf(InquiryListItemDto(inquiryId = "inq_$index")),
            page = InquiryListCursorDto(nextCursor = cursor, hasMore = hasMore),
        )
    }

    private class FakeInquirySource(
        private val pages: Map<String?, InquiryListPageDto>,
        private val cancelRequests: Boolean = false,
    ) : InquiryRemoteDataSource {
        val requestedCursors = mutableListOf<String?>()

        override suspend fun submitInquiry(title: String, content: String): ApiResult<InquirySubmissionResponseDto> {
            return ApiResult.Failure(ApiError.Unknown)
        }

        override suspend fun getInquiryPage(cursor: String?, limit: Int): ApiResult<InquiryListPageDto> {
            requestedCursors += cursor
            assertEquals(50, limit)
            if (cancelRequests) {
                throw CancellationException("Request cancelled")
            }
            val inquiryPage = pages[cursor] ?: return ApiResult.Failure(ApiError.Network)
            return ApiResult.Success(inquiryPage)
        }
    }
}
