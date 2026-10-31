package com.nativelap.heartguard.viewmodel.inquiry

import androidx.lifecycle.ViewModelStore
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import com.nativelap.heartguard.domain.inquiry.usecase.GetInquiriesUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InquiryViewModelTest {
    @Test
    fun `successful submission emits one list navigation effect`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeInquiryRepository()
        repository.submitResults += ApiResult.Success(SUBMISSION)
        val fixture = createFixture(repository)
        val effects = mutableListOf<InquiryEffect>()
        val effectCollector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.effects.collect(effects::add)
            }

        try {
            fixture.viewModel.updateTitle("제목")
            fixture.viewModel.updateContent("내용")
            fixture.viewModel.submitInquiry()
            advanceUntilIdle()

            assertEquals(1, repository.submitCallCount)
            assertEquals(0, repository.getCallCount)
            assertEquals(listOf<InquiryEffect>(InquiryEffect.NavigateToList), effects)
            assertEquals("", fixture.viewModel.uiState.value.title)
            assertEquals("", fixture.viewModel.uiState.value.content)
            assertFalse(fixture.viewModel.uiState.value.isSubmitting)
        } finally {
            effectCollector.cancel()
            fixture.viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed submission keeps input and permits a user retry`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeInquiryRepository()
        repository.submitResults += ApiResult.Failure(ApiError.Network)
        repository.submitResults += ApiResult.Success(SUBMISSION)
        val fixture = createFixture(repository)
        val effects = mutableListOf<InquiryEffect>()
        val effectCollector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.effects.collect(effects::add)
            }

        try {
            fixture.viewModel.updateTitle("제목")
            fixture.viewModel.updateContent("내용")
            fixture.viewModel.submitInquiry()
            advanceUntilIdle()

            assertEquals(1, repository.submitCallCount)
            assertEquals("제목", fixture.viewModel.uiState.value.title)
            assertEquals("내용", fixture.viewModel.uiState.value.content)
            assertTrue(fixture.viewModel.uiState.value.hasError)
            assertTrue(fixture.viewModel.uiState.value.canSubmitInquiry())
            assertTrue(effects.isEmpty())

            fixture.viewModel.submitInquiry()
            advanceUntilIdle()

            assertEquals(2, repository.submitCallCount)
            assertEquals(listOf<InquiryEffect>(InquiryEffect.NavigateToList), effects)
        } finally {
            effectCollector.cancel()
            fixture.viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `submission in progress blocks duplicate post calls`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeInquiryRepository()
        val pendingSubmission = CompletableDeferred<ApiResult<InquirySubmission>>()
        repository.submitBlock = { pendingSubmission.await() }
        val fixture = createFixture(repository)
        val effects = mutableListOf<InquiryEffect>()
        val effectCollector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.effects.collect(effects::add)
            }

        try {
            fixture.viewModel.updateTitle("제목")
            fixture.viewModel.updateContent("내용")
            fixture.viewModel.submitInquiry()
            runCurrent()

            assertEquals(1, repository.submitCallCount)
            assertTrue(fixture.viewModel.uiState.value.isSubmitting)

            fixture.viewModel.submitInquiry()
            runCurrent()

            assertEquals(1, repository.submitCallCount)
            pendingSubmission.complete(ApiResult.Success(SUBMISSION))
            advanceUntilIdle()

            assertEquals(1, repository.submitCallCount)
            assertEquals(listOf<InquiryEffect>(InquiryEffect.NavigateToList), effects)
        } finally {
            effectCollector.cancel()
            fixture.viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `opening list starts exactly one page request`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = FakeInquiryRepository()
        val fixture = createFixture(repository)

        try {
            fixture.viewModel.openInquiry()
            advanceUntilIdle()

            assertEquals(1, repository.getCallCount)
        } finally {
            fixture.viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }

    private fun createFixture(repository: FakeInquiryRepository): Fixture {
        val viewModelStore = ViewModelStore()
        val viewModel =
            InquiryViewModel(
                submitInquiryUseCase = SubmitInquiryUseCase(repository),
                getInquiriesUseCase = GetInquiriesUseCase(repository),
                sessionManager = SessionManager(InMemoryTokenStorage(), Dispatchers.Unconfined),
            )
        viewModelStore.put("inquiry", viewModel)
        return Fixture(viewModel, viewModelStore)
    }

    private class FakeInquiryRepository : InquiryRepository {
        var submitCallCount = 0
            private set
        var getCallCount = 0
            private set
        val submitResults = ArrayDeque<ApiResult<InquirySubmission>>()
        var submitBlock: suspend () -> ApiResult<InquirySubmission> = {
            submitResults.removeFirst()
        }

        override suspend fun submitInquiry(
            title: String,
            content: String,
        ): ApiResult<InquirySubmission> {
            submitCallCount += 1
            return submitBlock()
        }

        override suspend fun getInquiryPage(cursor: String?): ApiResult<InquiryPage> {
            getCallCount += 1
            return ApiResult.Success(InquiryPage(inquiries = emptyList(), nextCursor = null))
        }
    }

    private class InMemoryTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private data class Fixture(
        val viewModel: InquiryViewModel,
        val viewModelStore: ViewModelStore,
    )

    private companion object {
        val SUBMISSION = InquirySubmission("inquiry-id", "OPEN", "PENDING", "2026-10-31T12:00:00Z")
    }
}
