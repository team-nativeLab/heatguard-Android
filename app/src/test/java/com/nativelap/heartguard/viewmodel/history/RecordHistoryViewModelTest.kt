package com.nativelap.heartguard.viewmodel.history

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistorySliceUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
class RecordHistoryViewModelTest {
    // 2026-09-28 12:00 KST 고정 시계
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-09-28T03:00:00Z"),
        ZoneId.of("Asia/Seoul"),
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음 열면 최신 날짜 기록만 받고 스크롤로 기간 끝까지 이어 받는다`() = runTest {
        val repository = FakeRecordHistoryRepository()
        val viewModel = createViewModel(repository)

        viewModel.openHistory()
        advanceUntilIdle()

        assertEquals(listOf(LocalDate.of(2026, 9, 28)), repository.requestedDates)
        assertEquals(true, viewModel.uiState.value.hasMore)
        assertEquals(2, viewModel.uiState.value.recordCounts.total)

        viewModel.loadMoreRecords()
        advanceUntilIdle()
        viewModel.loadMoreRecords()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 9, 22), uiState.startDate)
        assertEquals(LocalDate.of(2026, 9, 28), uiState.endDate)
        assertEquals(7, repository.requestedDates.toSet().size)
        assertEquals(false, uiState.hasMore)
        assertEquals(RecordHistoryCounts(total = 3, thermometer = 1, work = 1, rest = 1), uiState.recordCounts)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 27)),
            uiState.dayGroups.map { dayGroup -> dayGroup.date },
        )
    }

    @Test
    fun `필터를 바꾸면 서버를 다시 부르지 않고 목록만 거른다`() = runTest {
        val repository = FakeRecordHistoryRepository()
        val viewModel = createViewModel(repository)
        viewModel.openHistory()
        advanceUntilIdle()
        while (viewModel.uiState.value.hasMore) {
            viewModel.loadMoreRecords()
            advanceUntilIdle()
        }
        val requestCountBeforeFilter = repository.requestedDates.size

        viewModel.selectFilter(RecordHistoryFilter.REST)

        val restEntries = viewModel.uiState.value.dayGroups.flatMap { dayGroup -> dayGroup.entries }
        assertEquals(listOf(FieldRecordType.REST), restEntries.map { recordEntry -> recordEntry.type })
        assertEquals(requestCountBeforeFilter, repository.requestedDates.size)
    }

    @Test
    fun `기간이 최대치를 넘거나 미래를 포함하면 오늘 기준으로 잘라 조회한다`() = runTest {
        val viewModel = createViewModel(FakeRecordHistoryRepository())

        viewModel.selectDateRange(
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 10, 5),
        )
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 9, 28), uiState.endDate)
        assertEquals(LocalDate.of(2026, 8, 29), uiState.startDate)
        assertEquals(false, uiState.isDateRangePickerVisible)
    }

    @Test
    fun `같은 날의 다음 커서를 이어 받고 실패하면 받은 목록을 유지한 채 재시도할 수 있다`() = runTest {
        val repository = FakeRecordHistoryRepository(splitLatestDay = true, failingCursor = "cur_2")
        val viewModel = createViewModel(repository)
        viewModel.openHistory()
        advanceUntilIdle()

        viewModel.loadMoreRecords()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.recordCounts.total)
        assertEquals(true, viewModel.uiState.value.hasLoadMoreError)
        assertEquals(true, viewModel.uiState.value.hasMore)

        repository.failingCursor = null
        viewModel.loadMoreRecords()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.recordCounts.total)
        assertEquals(false, viewModel.uiState.value.hasLoadMoreError)
    }

    @Test
    fun `서버가 받은 커서를 다시 주면 목록을 끝내고 실패를 알린다`() = runTest {
        val repository = FakeRecordHistoryRepository(splitLatestDay = true, repeatCursor = true)
        val viewModel = createViewModel(repository)
        viewModel.openHistory()
        advanceUntilIdle()

        viewModel.loadMoreRecords()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.hasMore)
        assertEquals(true, viewModel.uiState.value.hasLoadMoreError)
    }

    @Test
    fun `조회에 실패하면 Failed 상태가 된다`() = runTest {
        val viewModel = createViewModel(FakeRecordHistoryRepository(shouldFail = true))

        viewModel.openHistory()
        advanceUntilIdle()

        assertEquals(RecordHistoryLoadState.Failed, viewModel.uiState.value.loadState)
    }

    @Test
    fun `세션이 끝나면 조회 결과를 지운다`() = runTest {
        val sessionManager = createSessionManager()
        val viewModel = createViewModel(
            repository = FakeRecordHistoryRepository(),
            sessionManager = sessionManager,
        )
        viewModel.openHistory()
        advanceUntilIdle()

        sessionManager.expireSession()
        advanceUntilIdle()

        assertEquals(RecordHistoryLoadState.Loading, viewModel.uiState.value.loadState)
    }

    private suspend fun TestScope.createSessionManager(): SessionManager {
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return sessionManager
    }

    private suspend fun TestScope.createViewModel(
        repository: FakeRecordHistoryRepository,
        sessionManager: SessionManager? = null,
    ): RecordHistoryViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return RecordHistoryViewModel(
            getRecordHistorySliceUseCase = GetRecordHistorySliceUseCase(repository),
            clock = fixedClock,
            sessionManager = sessionManager ?: createSessionManager(),
        )
    }

    private class FakeRecordHistoryRepository(
        private val shouldFail: Boolean = false,
        private val splitLatestDay: Boolean = false,
        private val repeatCursor: Boolean = false,
        var failingCursor: String? = null,
    ) : RecordHistoryRepository {
        val requestedDates = mutableListOf<LocalDate>()

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> {
            requestedDates += date
            if (shouldFail || (cursor != null && cursor == failingCursor)) {
                return ApiResult.Failure(ApiError.Network)
            }
            val dailyEntries = dailyEntriesOf(date)
            if (!splitLatestDay || date != LocalDate.of(2026, 9, 28)) {
                return ApiResult.Success(RecordHistoryPage(entries = dailyEntries, nextCursor = null))
            }
            return ApiResult.Success(
                if (cursor == null) {
                    RecordHistoryPage(entries = dailyEntries.take(1), nextCursor = "cur_2")
                } else {
                    RecordHistoryPage(
                        entries = dailyEntries.drop(1),
                        nextCursor = if (repeatCursor) "cur_2" else null,
                    )
                },
            )
        }

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> {
            requestedDates += date
            if (shouldFail) {
                return ApiResult.Failure(ApiError.Network)
            }
            return ApiResult.Success(dailyEntriesOf(date))
        }

        private fun dailyEntriesOf(date: LocalDate): List<RecordHistoryEntry> {
            val dailyEntries = when (date) {
                LocalDate.of(2026, 9, 28) -> listOf(
                    recordEntry("rec_1", FieldRecordType.THERMOMETER, date),
                    recordEntry("rec_2", FieldRecordType.WORK, date),
                )

                LocalDate.of(2026, 9, 27) -> listOf(
                    recordEntry("rec_3", FieldRecordType.REST, date),
                )

                else -> emptyList()
            }
            return dailyEntries
        }

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            ApiResult.Failure(ApiError.Unknown)

        private fun recordEntry(
            recordId: String,
            recordType: FieldRecordType,
            measuredDate: LocalDate,
        ) = RecordHistoryEntry(
            recordId = recordId,
            type = recordType,
            temperature = null,
            humidity = null,
            apparentTemperature = null,
            heatLevel = null,
            photoCount = 1,
            photoUrls = emptyList(),
            memo = null,
            measuredAt = OffsetDateTime.of(measuredDate.atTime(10, 0), ZoneOffset.ofHours(9)),
        )
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
