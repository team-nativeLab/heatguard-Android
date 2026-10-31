package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelParallelRefreshTest {
    @Test
    fun `refresh starts record and site requests together and keeps site success when records fail`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val recordsStarted = CompletableDeferred<Unit>()
        val overviewStarted = CompletableDeferred<Unit>()
        val recordResult = CompletableDeferred<ApiResult<List<RecordHistoryEntry>>>()
        val overview = siteOverview("current site")
        val overviewResult = CompletableDeferred<ApiResult<TeamSiteOverview>>()
        val testViewModel =
            createHomeViewModel(
                recordRepository =
                    FakeRecordHistoryRepository {
                        recordsStarted.complete(Unit)
                        recordResult.await()
                    },
                siteRepository =
                    FakeTeamSiteRepository {
                        overviewStarted.complete(Unit)
                        overviewResult.await()
                    },
                dispatcher = dispatcher,
            )
        val viewModel = testViewModel.viewModel

        try {
            viewModel.refresh()
            runCurrent()

            assertTrue(recordsStarted.isCompleted)
            assertTrue(overviewStarted.isCompleted)

            recordResult.complete(ApiResult.Failure(ApiError.Network))
            overviewResult.complete(ApiResult.Success(overview))
            runCurrent()

            assertEquals(HomeUiState.Success(overview), viewModel.uiState.value)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `session transition prevents an older overview response from replacing the new session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val oldOverviewResult = CompletableDeferred<ApiResult<TeamSiteOverview>>()
        val newOverviewStarted = CompletableDeferred<Unit>()
        val oldOverview = siteOverview("old session")
        val newOverview = siteOverview("new session")
        var overviewCallCount = 0
        val testViewModel =
            createHomeViewModel(
                recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                siteRepository =
                    FakeTeamSiteRepository {
                        overviewCallCount += 1
                        if (overviewCallCount == 1) {
                            oldOverviewResult.await()
                        } else {
                            newOverviewStarted.complete(Unit)
                            ApiResult.Success(newOverview)
                        }
                    },
                dispatcher = dispatcher,
            )
        val viewModel = testViewModel.viewModel

        try {
            viewModel.refresh()
            runCurrent()
            assertEquals(1, overviewCallCount)

            testViewModel.sessionManager.onLoginSucceeded(SessionToken("new-session-token"))
            runCurrent()

            assertTrue(newOverviewStarted.isCompleted)
            assertEquals(HomeUiState.Success(newOverview), viewModel.uiState.value)

            oldOverviewResult.complete(ApiResult.Success(oldOverview))
            runCurrent()

            assertEquals(HomeUiState.Success(newOverview), viewModel.uiState.value)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `refresh cancellation reaches both in flight requests`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val recordGate = CompletableDeferred<ApiResult<List<RecordHistoryEntry>>>()
        val overviewGate = CompletableDeferred<ApiResult<TeamSiteOverview>>()
        var recordCallCount = 0
        var overviewCallCount = 0
        var recordRequestCancelled = false
        var overviewRequestCancelled = false
        val testViewModel =
            createHomeViewModel(
                recordRepository =
                    FakeRecordHistoryRepository {
                        recordCallCount += 1
                        if (recordCallCount == 1) {
                            try {
                                recordGate.await()
                            } catch (cancellationException: kotlinx.coroutines.CancellationException) {
                                recordRequestCancelled = true
                                throw cancellationException
                            }
                        } else {
                            ApiResult.Success(emptyList())
                        }
                    },
                siteRepository =
                    FakeTeamSiteRepository {
                        overviewCallCount += 1
                        if (overviewCallCount == 1) {
                            try {
                                overviewGate.await()
                            } catch (cancellationException: kotlinx.coroutines.CancellationException) {
                                overviewRequestCancelled = true
                                throw cancellationException
                            }
                        } else {
                            ApiResult.Success(siteOverview("refreshed site"))
                        }
                    },
                dispatcher = dispatcher,
            )
        val viewModel = testViewModel.viewModel

        try {
            viewModel.refresh()
            runCurrent()
            viewModel.refresh()
            runCurrent()

            assertTrue(recordRequestCancelled)
            assertTrue(overviewRequestCancelled)
            assertEquals(HomeUiState.Success(siteOverview("refreshed site")), viewModel.uiState.value)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private suspend fun createHomeViewModel(
        recordRepository: RecordHistoryRepository,
        siteRepository: TeamSiteRepository,
        dispatcher: CoroutineDispatcher,
    ): TestHomeViewModel {
        val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
        sessionManager.onLoginSucceeded(SessionToken("initial-session-token"))
        val viewModel =
            HomeViewModel(
                getTeamSiteOverviewUseCase = GetTeamSiteOverviewUseCase(siteRepository),
                getRecordHistoryUseCase = GetRecordHistoryUseCase(recordRepository),
                sessionManager = sessionManager,
                clock = Clock.fixed(java.time.Instant.parse("2026-10-31T03:00:00Z"), ZoneId.of("Asia/Seoul")),
            )
        return TestHomeViewModel(viewModel, sessionManager)
    }

    private fun siteOverview(siteName: String) =
        TeamSiteOverview(
            teamName = null,
            workplace = null,
            siteName = siteName,
            managerPhoneNumber = null,
            currentTemperature = null,
            humidity = null,
            apparentTemperature = null,
            heatLevel = null,
            checkTimes = emptyList(),
            hasActiveEmergencyCall = false,
        )

    private data class TestHomeViewModel(
        val viewModel: HomeViewModel,
        val sessionManager: SessionManager,
    )

    private class FakeTeamSiteRepository(
        private val getOverview: suspend () -> ApiResult<TeamSiteOverview>,
    ) : TeamSiteRepository {
        override suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview> = getOverview()
    }

    private class FakeRecordHistoryRepository(
        private val getRecords: suspend () -> ApiResult<List<RecordHistoryEntry>>,
    ) : RecordHistoryRepository {
        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> = getRecords()

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ) = error("Record page is not used in this test")

        override suspend fun getRecordDetail(recordId: String) = error("Record detail is not used in this test")
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
