package com.nativelap.heartguard.viewmodel.history

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordDayMarkersUseCase
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistorySliceUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RecordHistoryViewModelTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val today = LocalDate.of(2026, 11, 25)
    private val repository = DeferredRepository()
    private val session = SessionManager(MemoryTokenStorage(), Dispatchers.Unconfined, emptySet())
    private val clock = Clock.fixed(Instant.parse("2026-11-25T09:00:00Z"), ZoneOffset.UTC)

    @After
    fun cleanUp() {
        scope.cancel()
    }

    private suspend fun createViewModel(): RecordHistoryViewModel {
        session.onLoginSucceeded(SessionToken("account-one"))
        return RecordHistoryViewModel(
            GetRecordHistorySliceUseCase(repository),
            GetRecordDayMarkersUseCase(repository),
            clock,
            session,
            scope,
        ).also { yield() }
    }

    @Test
    fun selectedDateFirstPageIsSharedWithMarkerAndDuplicateLoadMoreIsBlocked() =
        runBlocking {
            val viewModel = createViewModel()
            viewModel.openHistory()
            assertEquals(1, repository.requests.count { it.date == today && it.cursor == null })
            repository.complete(today, null, ApiResult.Success(RecordHistoryPage(emptyList(), "next")))
            assertEquals(true, viewModel.uiState.value.recordDayMarkers[today])
            viewModel.loadMoreRecords()
            viewModel.loadMoreRecords()
            assertEquals(1, repository.requests.count { it.date == today && it.cursor == "next" })
            repository.complete(today, "next", ApiResult.Success(RecordHistoryPage(listOf(entry("next")), null)))
            val loadedEntries = (viewModel.uiState.value.loadState as RecordHistoryLoadState.Loaded).entries
            assertEquals(listOf("next"), loadedEntries.map { it.recordId })
        }

    @Test
    fun selectingAnotherDateRejectsCancelledLateResponse() =
        runBlocking {
            val viewModel = createViewModel()
            viewModel.openHistory()
            val oldRequest = repository.requests.first { it.date == today }
            val selected = today.minusDays(1)
            viewModel.selectDate(selected)
            val selectedRequest = repository.requests.last { it.date == selected && it.cursor == null }
            selectedRequest.response.complete(ApiResult.Success(RecordHistoryPage(listOf(entry("new")), null)))
            oldRequest.response.complete(ApiResult.Success(RecordHistoryPage(listOf(entry("old")), null)))
            assertEquals(selected, viewModel.uiState.value.selectedDate)
            val loadedEntries = (viewModel.uiState.value.loadState as RecordHistoryLoadState.Loaded).entries
            assertEquals(listOf("new"), loadedEntries.map { it.recordId })
            assertFalse(
                viewModel.uiState.value.recordDayMarkers
                    .containsKey(today),
            )
        }

    @Test
    fun accountSwitchDiscardsInFlightOldAccountResponse() =
        runBlocking {
            val viewModel = createViewModel()
            viewModel.openHistory()
            val oldRequest = repository.requests.first { it.date == today }
            session.onLoginSucceeded(SessionToken("account-two"))
            repeat(4) { yield() }
            oldRequest.response.complete(ApiResult.Success(RecordHistoryPage(listOf(entry("old")), null)))
            repeat(4) { yield() }
            assertTrue(viewModel.uiState.value.loadState is RecordHistoryLoadState.Loading)
            assertTrue(
                viewModel.uiState.value.recordDayMarkers
                    .isEmpty(),
            )
        }

    @Test
    fun failedFirstPageRetriesMarkerAndUserCanRetryList() =
        runBlocking {
            val viewModel = createViewModel()
            viewModel.openHistory()
            repository.complete(today, null, ApiResult.Failure(ApiError.Network))
            assertTrue(viewModel.uiState.value.loadState is RecordHistoryLoadState.Failed)
            assertEquals(2, repository.requests.count { it.date == today && it.cursor == null })
            repository.complete(today, null, ApiResult.Success(RecordHistoryPage(emptyList(), null)))
            repository.requests.filter { it.date != today }.toList().forEach { request ->
                request.response.complete(ApiResult.Success(RecordHistoryPage(emptyList(), null)))
            }
            assertEquals(false, viewModel.uiState.value.recordDayMarkers[today])
            viewModel.loadRecords()
            repository.complete(today, null, ApiResult.Success(RecordHistoryPage(listOf(entry("retry")), null)))
            val loadedEntries = (viewModel.uiState.value.loadState as RecordHistoryLoadState.Loaded).entries
            assertEquals(listOf("retry"), loadedEntries.map { it.recordId })
            assertEquals(true, viewModel.uiState.value.recordDayMarkers[today])
        }

    private fun entry(id: String) =
        RecordHistoryEntry(
            recordId = id,
            type = FieldRecordType.WORK,
            temperature = null,
            humidity = null,
            apparentTemperature = null,
            heatLevel = null,
            photoCount = 0,
            photoUrls = emptyList(),
            memo = null,
            measuredAt = null,
        )

    private class MemoryTokenStorage : TokenStorage {
        private var token: String? = null
        private var identifier: String? = null

        override fun readAccessToken(): String? = token

        override fun saveAccessToken(accessToken: String) {
            token = accessToken
        }

        override fun readSessionIdentifier(): String? = identifier

        override fun saveSessionIdentifier(identifier: String) {
            this.identifier = identifier
        }

        override fun clear() {
            token = null
        }
    }

    private class DeferredRepository : RecordHistoryRepository {
        data class Request(
            val date: LocalDate,
            val cursor: String?,
            val response: CompletableDeferred<ApiResult<RecordHistoryPage>> = CompletableDeferred(),
        )

        val requests = mutableListOf<Request>()

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> {
            val request = Request(date, cursor)
            requests += request
            return withContext(NonCancellable) { request.response.await() }
        }

        fun complete(
            date: LocalDate,
            cursor: String?,
            response: ApiResult<RecordHistoryPage>,
        ) {
            val request = requests.first { it.date == date && it.cursor == cursor && !it.response.isCompleted }
            request.response.complete(response)
        }

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> =
            error("Unexpected")

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> = error("Unexpected")
    }
}
