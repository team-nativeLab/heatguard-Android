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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecordHistoryViewModelTest {
    // 2026-09-27은 일요일이다. 주간 스트립은 9월 21일(월)~27일(일)이다.
    private val today = LocalDate.of(2026, 9, 27)
    private val seoulZone: ZoneId = ZoneId.of("Asia/Seoul")

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a future date is ignored and the selected day stays`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeHistoryRepository()
            val viewModel = createViewModel(repository)

            viewModel.openHistory()
            advanceUntilIdle()
            viewModel.selectDate(today.plusDays(1))
            advanceUntilIdle()

            assertEquals(today, viewModel.uiState.value.selectedDate)
        }

    @Test
    fun `selecting a past day shows only that day's records and closes the picker`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val pastDay = LocalDate.of(2026, 9, 20)
            val repository =
                FakeHistoryRepository(
                    initialPages =
                        mapOf(
                            pastDay to
                                ApiResult.Success(
                                    RecordHistoryPage(listOf(sampleEntry("rec_0920", pastDay)), null),
                                ),
                        ),
                )
            val viewModel = createViewModel(repository)
            viewModel.openHistory()
            advanceUntilIdle()
            viewModel.showDatePicker()

            viewModel.selectDate(pastDay)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(pastDay, state.selectedDate)
            assertEquals(YearMonth.of(2026, 9), state.visibleMonth)
            assertFalse(state.isDatePickerVisible)
            assertEquals(
                listOf("rec_0920"),
                state.dayGroups
                    .flatMap { dayGroup ->
                        dayGroup.entries
                    }.map { it.recordId },
            )
        }

    @Test
    fun `visible month cannot move past the current month`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = createViewModel(FakeHistoryRepository())
            viewModel.openHistory()
            advanceUntilIdle()

            viewModel.moveVisibleMonth(1)
            assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.visibleMonth)

            viewModel.moveVisibleMonth(-1)
            assertEquals(YearMonth.of(2026, 8), viewModel.uiState.value.visibleMonth)
        }

    @Test
    fun `failed marker day is absent, empty day is false, and a day is not requested again once cached`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val failedDay = LocalDate.of(2026, 9, 10)
            val recordDay = LocalDate.of(2026, 9, 11)
            val repository =
                FakeHistoryRepository(
                    initialPages =
                        mapOf(
                            failedDay to ApiResult.Failure(ApiError.Network),
                            recordDay to
                                ApiResult.Success(RecordHistoryPage(listOf(sampleEntry("rec_0911", recordDay)), null)),
                        ),
                )
            val viewModel = createViewModel(repository)

            viewModel.openHistory()
            advanceUntilIdle()
            viewModel.showDatePicker()
            advanceUntilIdle()

            val markers = viewModel.uiState.value.recordDayMarkers
            assertNull(markers[failedDay])
            assertEquals(true, markers[recordDay])
            assertEquals(false, markers[LocalDate.of(2026, 9, 12)])
            assertEquals(26, markers.size)

            viewModel.moveVisibleMonth(-1)
            advanceUntilIdle()
            viewModel.moveVisibleMonth(1)
            advanceUntilIdle()

            assertEquals(1, repository.requestCount(LocalDate.of(2026, 9, 12)))
            assertTrue(repository.requestCount(LocalDate.of(2026, 9, 28)) == 0)
        }

    @Test
    fun `opening history requests record markers only for the selected week while the date sheet is closed`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeHistoryRepository()
            val viewModel = createViewModel(repository)

            viewModel.openHistory()
            advanceUntilIdle()

            assertEquals(weekOf(today), viewModel.uiState.value.recordDayMarkers.keys)
            assertEquals(0, repository.requestCount(LocalDate.of(2026, 9, 1)))
            assertEquals(0, repository.requestCount(LocalDate.of(2026, 9, 20)))
        }

    @Test
    fun `opening the date sheet requests record markers for the whole visible month`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeHistoryRepository()
            val viewModel = createViewModel(repository)
            viewModel.openHistory()
            advanceUntilIdle()

            viewModel.showDatePicker()
            advanceUntilIdle()

            assertEquals(1, repository.requestCount(LocalDate.of(2026, 9, 1)))
            assertEquals(dateRange(LocalDate.of(2026, 9, 1), today), viewModel.uiState.value.recordDayMarkers.keys)
        }

    @Test
    fun `record marker dates are the visible month and selected week without future dates while the sheet is open`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = createViewModel(FakeHistoryRepository(), currentDate = LocalDate.of(2026, 9, 23))
            viewModel.openHistory()
            advanceUntilIdle()
            // 9월 1일(화)이 속한 주는 8월 31일부터 시작하고, 9월 24일 이후는 오늘 이후라 뺀다.
            viewModel.selectDate(LocalDate.of(2026, 9, 1))
            advanceUntilIdle()

            viewModel.showDatePicker()

            assertEquals(
                dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 23)) + LocalDate.of(2026, 8, 31),
                viewModel.uiState.value.recordMarkerDates,
            )
        }

    @Test
    fun `record marker dates cover the previous month and the selected week across the month boundary`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = createViewModel(FakeHistoryRepository(), currentDate = LocalDate.of(2026, 9, 23))
            viewModel.openHistory()
            advanceUntilIdle()
            viewModel.selectDate(LocalDate.of(2026, 9, 1))
            advanceUntilIdle()
            viewModel.showDatePicker()
            viewModel.moveVisibleMonth(-1)

            // 8월을 보는 중이면 8월 전체와, 선택한 주(8/31~9/6)가 걸친 9월 1일~6일을 함께 요청한다.
            assertEquals(
                dateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)) +
                    dateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 6)),
                viewModel.uiState.value.recordMarkerDates,
            )
        }

    @Test
    fun `reopening history drops cached and in-flight record markers before requesting again`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val recordDay = LocalDate.of(2026, 9, 22)
            val repository =
                FakeHistoryRepository(
                    initialPages =
                        mapOf(
                            recordDay to
                                ApiResult.Success(RecordHistoryPage(listOf(sampleEntry("rec_0922", recordDay)), null)),
                        ),
                )
            val viewModel = createViewModel(repository)
            // 첫 진입의 요청은 응답 전에 멈춰 두고, 서버 상태가 기록 없음으로 바뀌었다고 가정한다.
            val staleGate = CompletableDeferred<Unit>()
            repository.gate = staleGate
            viewModel.openHistory()
            runCurrent()
            repository.setPage(recordDay, ApiResult.Success(RecordHistoryPage(emptyList(), null)))
            val freshGate = CompletableDeferred<Unit>()
            repository.gate = freshGate

            viewModel.openHistory()
            runCurrent()

            val markers = viewModel.uiState.value.recordDayMarkers
            assertTrue(markers.isEmpty())
            freshGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.recordDayMarkers[recordDay])
            assertEquals(2, repository.requestCount(recordDay))

            staleGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.recordDayMarkers[recordDay])
        }

    @Test
    fun `ending the session clears record day markers so the next session requests them again`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val recordDay = LocalDate.of(2026, 9, 22)
            val repository =
                FakeHistoryRepository(
                    initialPages =
                        mapOf(
                            recordDay to
                                ApiResult.Success(RecordHistoryPage(listOf(sampleEntry("rec_0922", recordDay)), null)),
                        ),
                )
            val sessionManager = newSessionManager()
            sessionManager.onLoginSucceeded(SessionToken("first-session-token"))
            val viewModel = newViewModel(repository, sessionManager, today)
            viewModel.openHistory()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.recordDayMarkers[recordDay])

            sessionManager.expireSession()
            advanceUntilIdle()
            val markers = viewModel.uiState.value.recordDayMarkers
            assertTrue(markers.isEmpty())

            // 다음 계정의 서버 상태가 달라졌을 때 이전 계정의 점이 남아 있으면 안 된다.
            repository.setPage(recordDay, ApiResult.Success(RecordHistoryPage(emptyList(), null)))
            sessionManager.onLoginSucceeded(SessionToken("second-session-token"))
            advanceUntilIdle()
            viewModel.showDatePicker()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.recordDayMarkers[recordDay])
            assertEquals(2, repository.requestCount(recordDay))
        }

    @Test
    fun `record marker results from an older session generation are dropped`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val recordDay = LocalDate.of(2026, 9, 22)
            val repository =
                FakeHistoryRepository(
                    initialPages =
                        mapOf(
                            recordDay to
                                ApiResult.Success(RecordHistoryPage(listOf(sampleEntry("rec_0922", recordDay)), null)),
                        ),
                )
            // 로그인 전에 열어 응답을 기다리게 한 뒤 계정을 만든다. 이 세대 변경은 정리 수집자가 작업을 취소하지 않으므로
            // 도착한 응답을 세대 검사가 버리는지만 확인한다.
            val sessionManager = newSessionManager()
            val viewModel = newViewModel(repository, sessionManager, today)
            val staleGate = CompletableDeferred<Unit>()
            repository.gate = staleGate
            viewModel.openHistory()
            runCurrent()

            sessionManager.onLoginSucceeded(SessionToken("test-session-token"))
            staleGate.complete(Unit)
            advanceUntilIdle()

            val markers = viewModel.uiState.value.recordDayMarkers
            assertTrue(markers.isEmpty())

            repository.gate = null
            viewModel.openHistory()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.recordDayMarkers[recordDay])
        }

    private suspend fun TestScope.createViewModel(
        repository: RecordHistoryRepository,
        currentDate: LocalDate = today,
    ): RecordHistoryViewModel {
        val sessionManager = newSessionManager()
        sessionManager.onLoginSucceeded(SessionToken("test-session-token"))
        return newViewModel(repository, sessionManager, currentDate)
    }

    private fun TestScope.newSessionManager(): SessionManager =
        SessionManager(TestTokenStorage(), StandardTestDispatcher(testScheduler))

    private fun TestScope.newViewModel(
        repository: RecordHistoryRepository,
        sessionManager: SessionManager,
        currentDate: LocalDate,
    ): RecordHistoryViewModel =
        RecordHistoryViewModel(
            getRecordHistorySliceUseCase = GetRecordHistorySliceUseCase(repository),
            getRecordDayMarkersUseCase = GetRecordDayMarkersUseCase(repository),
            clock = fixedClock(currentDate),
            sessionManager = sessionManager,
        )

    private fun fixedClock(date: LocalDate): Clock =
        Clock.fixed(date.atTime(LocalTime.NOON).atZone(seoulZone).toInstant(), seoulZone)

    private fun weekOf(date: LocalDate): Set<LocalDate> {
        val monday = date.minusDays(date.dayOfWeek.value.toLong() - 1)
        return dateRange(monday, monday.plusDays(6)).filterNot { day -> day.isAfter(today) }.toSet()
    }

    private fun dateRange(
        start: LocalDate,
        end: LocalDate,
    ): Set<LocalDate> =
        generateSequence(start) { date -> date.plusDays(1) }
            .takeWhile { date -> !date.isAfter(end) }
            .toSet()

    private fun sampleEntry(
        recordId: String,
        date: LocalDate,
    ) = RecordHistoryEntry(
        recordId = recordId,
        type = FieldRecordType.THERMOMETER,
        temperature = 36.2,
        humidity = 65.0,
        apparentTemperature = 38.7,
        heatLevel = 1,
        photoCount = 0,
        photoUrls = emptyList(),
        memo = null,
        measuredAt = OffsetDateTime.parse("${date}T14:02:00+09:00"),
    )

    private class FakeHistoryRepository(
        initialPages: Map<LocalDate, ApiResult<RecordHistoryPage>> = emptyMap(),
    ) : RecordHistoryRepository {
        private val pages = initialPages.toMutableMap()
        private val requestedDates = mutableListOf<LocalDate>()

        // 설정돼 있으면 응답을 미리 정한 뒤 이 게이트가 열릴 때까지 요청을 멈춘다.
        var gate: CompletableDeferred<Unit>? = null

        fun requestCount(date: LocalDate): Int = requestedDates.count { requested -> requested == date }

        fun setPage(
            date: LocalDate,
            result: ApiResult<RecordHistoryPage>,
        ) {
            pages[date] = result
        }

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> =
            error("Not used by the history view model")

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> {
            requestedDates += date
            val response = pages[date] ?: ApiResult.Success(RecordHistoryPage(emptyList(), null))
            gate?.await()
            return response
        }

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            error("Record detail is not used in this test")
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
