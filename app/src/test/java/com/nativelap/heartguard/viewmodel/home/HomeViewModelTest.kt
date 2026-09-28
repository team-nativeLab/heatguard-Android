package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    // 2026-09-28 14:00 KST
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-28T05:00:00Z"), ZoneId.of("Asia/Seoul"))

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `처음 조회에 실패하면 Error 상태가 된다`() = runTest {
        val teamSiteRepository = FakeTeamSiteRepository(ApiResult.Failure(ApiError.Network))
        val viewModel = createViewModel(teamSiteRepository)

        viewModel.loadTeamSiteOverview()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HomeUiState.Error)
    }

    @Test
    fun `이미 값이 있을 때 새로고침만 실패하면 이전 값을 유지한다`() = runTest {
        val teamSiteRepository = FakeTeamSiteRepository(ApiResult.Success(overview()))
        val viewModel = createViewModel(teamSiteRepository)
        viewModel.loadTeamSiteOverview()
        advanceUntilIdle()

        teamSiteRepository.overviewResult = ApiResult.Failure(ApiError.Network)
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(HomeUiState.Success(overview()), viewModel.uiState.value)
    }

    @Test
    fun `오늘 기록 측정 시각으로 체크 완료를 표시한다`() = runTest {
        val viewModel = createViewModel(FakeTeamSiteRepository(ApiResult.Success(overview())))

        viewModel.loadTeamSiteOverview()
        advanceUntilIdle()

        val checkSchedule = viewModel.checkSchedule.filterNotNull().first()
        assertEquals(setOf(LocalTime.of(9, 0)), checkSchedule.completedCheckTimes)
    }

    private fun overview() = TeamSiteOverview(
        teamName = "철근팀",
        workplace = "3층 외벽",
        siteName = "서울현장",
        managerPhoneNumber = null,
        currentTemperature = null,
        humidity = null,
        apparentTemperature = null,
        heatLevel = null,
        checkTimes = listOf("09:00", "11:00"),
        hasActiveEmergencyCall = false,
    )

    private fun TestScope.createViewModel(teamSiteRepository: FakeTeamSiteRepository): HomeViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return HomeViewModel(
            getTeamSiteOverviewUseCase = GetTeamSiteOverviewUseCase(teamSiteRepository),
            getRecordHistoryUseCase = GetRecordHistoryUseCase(FakeRecordHistoryRepository()),
            clock = fixedClock,
        )
    }

    private class FakeTeamSiteRepository(
        var overviewResult: ApiResult<TeamSiteOverview>,
    ) : TeamSiteRepository {
        override suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview> = overviewResult
    }

    // 오늘 09:30(KST)에 기록 1건이 있다.
    private class FakeRecordHistoryRepository : RecordHistoryRepository {
        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> = ApiResult.Success(
            listOf(
                RecordHistoryEntry(
                    recordId = "rec_01",
                    type = FieldRecordType.WORK,
                    temperature = null,
                    humidity = null,
                    apparentTemperature = null,
                    heatLevel = null,
                    photoCount = 1,
                    photoUrls = emptyList(),
                    memo = null,
                    measuredAt = OffsetDateTime.parse("${date}T09:30:00+09:00"),
                ),
            ),
        )

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            ApiResult.Failure(ApiError.Unknown)
    }
}
