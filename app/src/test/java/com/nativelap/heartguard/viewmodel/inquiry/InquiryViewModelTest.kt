package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import com.nativelap.heartguard.domain.inquiry.usecase.GetInquiriesUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InquiryViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `화면에 들어오면 문의 목록을 최신 등록순으로 불러온다`() = runTest {
        val repository = FakeInquiryRepository()
        repository.storedInquiries += inquirySummary("inq_old", "2026-09-20T01:00:00Z")
        repository.storedInquiries += inquirySummary("inq_new", "2026-09-27T01:00:00Z")
        val viewModel = createViewModel(repository)

        viewModel.openInquiry()
        advanceUntilIdle()

        val listState = viewModel.uiState.value.listState as InquiryListState.Loaded
        assertEquals(listOf("inq_new", "inq_old"), listState.inquiries.map { inquiry -> inquiry.inquiryId })
    }

    @Test
    fun `문의를 등록하면 입력을 비우고 목록을 다시 불러온다`() = runTest {
        val repository = FakeInquiryRepository()
        val viewModel = createViewModel(repository)
        viewModel.openInquiry()
        advanceUntilIdle()

        viewModel.updateTitle("점검 문의")
        viewModel.updateContent("내용")
        viewModel.submitInquiry()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals("", uiState.title)
        assertEquals("", uiState.content)
        assertEquals("inq_created", uiState.submission?.inquiryId)
        val listState = uiState.listState as InquiryListState.Loaded
        assertEquals(listOf("inq_created"), listState.inquiries.map { inquiry -> inquiry.inquiryId })
    }

    @Test
    fun `목록 조회에 실패하면 Failed 상태가 된다`() = runTest {
        val viewModel = createViewModel(FakeInquiryRepository(shouldFailList = true))

        viewModel.openInquiry()
        advanceUntilIdle()

        assertEquals(InquiryListState.Failed, viewModel.uiState.value.listState)
    }

    @Test
    fun `목록 끝에서 다음 페이지를 이어 받고 실패하면 받은 목록을 유지한다`() = runTest {
        val repository = FakeInquiryRepository(pageSize = 2, failingCursor = "cur_2")
        repeat(3) { inquiryIndex ->
            repository.storedInquiries += inquirySummary("inq_$inquiryIndex", "2026-09-2${inquiryIndex}T01:00:00Z")
        }
        val viewModel = createViewModel(repository)
        viewModel.openInquiry()
        advanceUntilIdle()

        viewModel.loadMoreInquiries()
        advanceUntilIdle()
        val failedState = viewModel.uiState.value.listState as InquiryListState.Loaded
        assertEquals(2, failedState.inquiries.size)
        assertEquals(true, failedState.hasLoadMoreError)

        repository.failingCursor = null
        viewModel.loadMoreInquiries()
        advanceUntilIdle()
        val loadedState = viewModel.uiState.value.listState as InquiryListState.Loaded
        assertEquals(3, loadedState.inquiries.size)
        assertEquals(null, loadedState.nextCursor)
        assertEquals(listOf(null, "cur_2", "cur_2"), repository.requestedCursors)
    }

    @Test
    fun `세션이 끝나면 입력과 목록을 지운다`() = runTest {
        val sessionManager = createSessionManager()
        val viewModel = createViewModel(
            repository = FakeInquiryRepository(),
            sessionManager = sessionManager,
        )
        viewModel.openInquiry()
        viewModel.updateTitle("이전 작업자 문의")
        advanceUntilIdle()

        sessionManager.expireSession()
        advanceUntilIdle()

        assertEquals(InquiryUiState(), viewModel.uiState.value)
    }

    private fun inquirySummary(
        inquiryId: String,
        createdAt: String,
    ) = InquirySummary(
        inquiryId = inquiryId,
        title = inquiryId,
        status = InquiryStatus.OPEN,
        replyCount = 0,
        createdAt = OffsetDateTime.parse(createdAt),
    )

    private suspend fun TestScope.createSessionManager(): SessionManager {
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return sessionManager
    }

    private suspend fun TestScope.createViewModel(
        repository: FakeInquiryRepository,
        sessionManager: SessionManager? = null,
    ): InquiryViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return InquiryViewModel(
            submitInquiryUseCase = SubmitInquiryUseCase(repository),
            getInquiriesUseCase = GetInquiriesUseCase(repository),
            sessionManager = sessionManager ?: createSessionManager(),
        )
    }

    private inner class FakeInquiryRepository(
        private val shouldFailList: Boolean = false,
        private val pageSize: Int = Int.MAX_VALUE,
        var failingCursor: String? = null,
    ) : InquiryRepository {
        val storedInquiries = mutableListOf<InquirySummary>()
        val requestedCursors = mutableListOf<String?>()

        override suspend fun submitInquiry(
            title: String,
            content: String,
        ): ApiResult<InquirySubmission> {
            storedInquiries += inquirySummary("inq_created", "2026-09-28T01:00:00Z")
            return ApiResult.Success(
                InquirySubmission(
                    inquiryId = "inq_created",
                    status = "OPEN",
                    deliveryStatus = "PENDING",
                    createdAt = "2026-09-28T01:00:00Z",
                ),
            )
        }

        override suspend fun getInquiryPage(cursor: String?): ApiResult<InquiryPage> {
            requestedCursors += cursor
            if (shouldFailList || (cursor != null && cursor == failingCursor)) {
                return ApiResult.Failure(ApiError.Network)
            }
            val startIndex = cursor?.removePrefix("cur_")?.toInt() ?: 0
            val endIndex = minOf(startIndex + pageSize, storedInquiries.size)
            return ApiResult.Success(
                InquiryPage(
                    inquiries = storedInquiries.subList(startIndex, endIndex).toList(),
                    nextCursor = "cur_$endIndex".takeIf { endIndex < storedInquiries.size },
                ),
            )
        }
    }

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = "access-token"

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
