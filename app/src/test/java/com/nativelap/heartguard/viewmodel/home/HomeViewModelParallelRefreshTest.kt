package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.location.CurrentCoordinates
import com.nativelap.heartguard.core.location.CurrentLocationProvider
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import com.nativelap.heartguard.domain.site.usecase.GetCurrentLocationWeatherUseCase
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelParallelRefreshTest {
    @Test
    fun `refresh starts record and site requests together and keeps site success when records fail`() =
        runTest {
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
    fun `session transition prevents an older overview response from replacing the new session`() =
        runTest {
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
    fun `repeated refresh keeps in flight requests instead of restarting them`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val recordGate = CompletableDeferred<ApiResult<List<RecordHistoryEntry>>>()
            val overviewGate = CompletableDeferred<ApiResult<TeamSiteOverview>>()
            var recordCallCount = 0
            var overviewCallCount = 0
            var isRecordRequestCancelled = false
            var isOverviewRequestCancelled = false
            val testViewModel =
                createHomeViewModel(
                    recordRepository =
                        FakeRecordHistoryRepository {
                            recordCallCount += 1
                            try {
                                recordGate.await()
                            } catch (cancellationException: CancellationException) {
                                isRecordRequestCancelled = true
                                throw cancellationException
                            }
                        },
                    siteRepository =
                        FakeTeamSiteRepository {
                            overviewCallCount += 1
                            try {
                                overviewGate.await()
                            } catch (cancellationException: CancellationException) {
                                isOverviewRequestCancelled = true
                                throw cancellationException
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

                assertFalse(isRecordRequestCancelled)
                assertFalse(isOverviewRequestCancelled)
                assertEquals(1, recordCallCount)
                assertEquals(1, overviewCallCount)

                recordGate.complete(ApiResult.Success(emptyList()))
                overviewGate.complete(ApiResult.Success(siteOverview("first site")))
                runCurrent()

                assertEquals(HomeUiState.Success(siteOverview("first site")), viewModel.uiState.value)
                assertEquals(1, viewModel.refreshCompletionCount.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `loadTeamSiteOverview cancels both in flight requests and restarts them`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val recordGate = CompletableDeferred<ApiResult<List<RecordHistoryEntry>>>()
            val overviewGate = CompletableDeferred<ApiResult<TeamSiteOverview>>()
            var recordCallCount = 0
            var overviewCallCount = 0
            var isRecordRequestCancelled = false
            var isOverviewRequestCancelled = false
            val testViewModel =
                createHomeViewModel(
                    recordRepository =
                        FakeRecordHistoryRepository {
                            recordCallCount += 1
                            if (recordCallCount == 1) {
                                try {
                                    recordGate.await()
                                } catch (cancellationException: CancellationException) {
                                    isRecordRequestCancelled = true
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
                                } catch (cancellationException: CancellationException) {
                                    isOverviewRequestCancelled = true
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
                viewModel.loadTeamSiteOverview()
                runCurrent()

                assertTrue(isRecordRequestCancelled)
                assertTrue(isOverviewRequestCancelled)
                assertEquals(2, overviewCallCount)
                assertEquals(HomeUiState.Success(siteOverview("refreshed site")), viewModel.uiState.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `refresh starts a new load when the running load belongs to an older session generation`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val oldOverviewGate = CompletableDeferred<ApiResult<TeamSiteOverview>>()
            var overviewCallCount = 0
            val sessionManager =
                SessionManager(
                    TestTokenStorage(storedAccessToken = "restored-session-token"),
                    dispatcher,
                )
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository =
                        FakeTeamSiteRepository {
                            overviewCallCount += 1
                            if (overviewCallCount == 1) {
                                oldOverviewGate.await()
                            } else {
                                ApiResult.Success(siteOverview("current session"))
                            }
                        },
                    dispatcher = dispatcher,
                    sessionManager = sessionManager,
                )
            val viewModel = testViewModel.viewModel

            try {
                viewModel.refresh()
                runCurrent()
                assertEquals(1, overviewCallCount)

                // 복원 세션의 첫 인증은 상태 정리를 일으키지 않으므로 이전 세대의 조회가 그대로 남는다.
                sessionManager.initialize()
                runCurrent()
                viewModel.refresh()
                runCurrent()

                assertEquals(2, overviewCallCount)
                assertEquals(HomeUiState.Success(siteOverview("current session")), viewModel.uiState.value)

                oldOverviewGate.complete(ApiResult.Success(siteOverview("old session")))
                runCurrent()

                assertEquals(HomeUiState.Success(siteOverview("current session")), viewModel.uiState.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `loadTeamSiteOverviewIfNeeded keeps the running first load when the main flow is recreated`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val overviewGate = CompletableDeferred<ApiResult<TeamSiteOverview>>()
            var overviewCallCount = 0
            var isOverviewRequestCancelled = false
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository =
                        FakeTeamSiteRepository {
                            overviewCallCount += 1
                            try {
                                overviewGate.await()
                            } catch (cancellationException: CancellationException) {
                                isOverviewRequestCancelled = true
                                throw cancellationException
                            }
                        },
                    dispatcher = dispatcher,
                )
            val viewModel = testViewModel.viewModel

            try {
                viewModel.loadTeamSiteOverviewIfNeeded()
                runCurrent()
                // 회전으로 메인 흐름이 다시 구성되면 같은 진입 호출이 한 번 더 들어온다.
                viewModel.loadTeamSiteOverviewIfNeeded()
                runCurrent()

                assertEquals(1, overviewCallCount)
                assertFalse(isOverviewRequestCancelled)
                assertEquals(HomeUiState.Loading, viewModel.uiState.value)

                overviewGate.complete(ApiResult.Success(siteOverview("first site")))
                runCurrent()
                viewModel.loadTeamSiteOverviewIfNeeded()
                runCurrent()

                assertEquals(1, overviewCallCount)
                assertEquals(HomeUiState.Success(siteOverview("first site")), viewModel.uiState.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `available location weather becomes the shared home and record value`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository =
                        FakeTeamSiteRepository(
                            getOverview = {
                                ApiResult.Success(
                                    siteOverview("team").copy(
                                        currentTemperature = 22.0,
                                        humidity = 44.0,
                                    ),
                                )
                            },
                            getWeather = { _, _ -> ApiResult.Success(locationWeather(WeatherAvailability.AVAILABLE)) },
                        ),
                    dispatcher = dispatcher,
                    currentLocationProvider = { CurrentCoordinates(37.5665, 126.978) },
                )
            val viewModel = testViewModel.viewModel
            val siteStatusCollection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.siteStatus.collect() }

            try {
                viewModel.refresh()
                runCurrent()
                viewModel.loadCurrentLocationWeather()
                runCurrent()

                assertEquals("31", viewModel.siteStatus.value.temperature)
                assertEquals(31.0, viewModel.siteStatus.value.recordTemperature)
                assertEquals(55.0, viewModel.siteStatus.value.recordHumidity)
            } finally {
                siteStatusCollection.cancel()
                advanceTimeBy(5_001L)
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `clearing location weather after permission revoke restores team values and ignores in flight response`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val lateWeatherResult = CompletableDeferred<ApiResult<CurrentLocationWeather>>()
            var weatherCallCount = 0
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository =
                        FakeTeamSiteRepository(
                            getOverview = {
                                ApiResult.Success(
                                    siteOverview("team").copy(
                                        currentTemperature = 22.0,
                                        humidity = 44.0,
                                    ),
                                )
                            },
                            getWeather = { _, _ ->
                                weatherCallCount += 1
                                if (weatherCallCount == 1) {
                                    ApiResult.Success(locationWeather(WeatherAvailability.AVAILABLE))
                                } else {
                                    lateWeatherResult.await()
                                }
                            },
                        ),
                    dispatcher = dispatcher,
                    currentLocationProvider = { CurrentCoordinates(37.5665, 126.978) },
                )
            val viewModel = testViewModel.viewModel
            val siteStatusCollection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.siteStatus.collect() }

            try {
                viewModel.refresh()
                runCurrent()
                viewModel.loadCurrentLocationWeather()
                runCurrent()
                assertEquals(31.0, viewModel.siteStatus.value.recordTemperature)

                viewModel.loadCurrentLocationWeather()
                runCurrent()
                assertEquals(2, weatherCallCount)

                viewModel.clearCurrentLocationWeather()
                lateWeatherResult.complete(ApiResult.Success(locationWeather(WeatherAvailability.CACHED)))
                runCurrent()

                assertEquals("22", viewModel.siteStatus.value.temperature)
                assertEquals(22.0, viewModel.siteStatus.value.recordTemperature)
                assertEquals(44.0, viewModel.siteStatus.value.recordHumidity)
            } finally {
                siteStatusCollection.cancel()
                advanceTimeBy(5_001L)
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `granted location request keeps team values on unavailable and failed responses`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            var locationCalls = 0
            var weatherResult: ApiResult<CurrentLocationWeather> =
                ApiResult.Success(locationWeather(WeatherAvailability.UNAVAILABLE))
            val repository =
                FakeTeamSiteRepository(
                    getOverview = {
                        ApiResult.Success(
                            siteOverview("team").copy(
                                currentTemperature = 22.0,
                                humidity = 44.0,
                            ),
                        )
                    },
                    getWeather = { latitude, longitude ->
                        assertEquals(37.5665, latitude)
                        assertEquals(126.978, longitude)
                        weatherResult
                    },
                )
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository = repository,
                    dispatcher = dispatcher,
                    currentLocationProvider = {
                        locationCalls += 1
                        CurrentCoordinates(37.5665, 126.978)
                    },
                )
            val viewModel = testViewModel.viewModel
            val siteStatusCollection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.siteStatus.collect() }

            try {
                viewModel.refresh()
                runCurrent()
                viewModel.loadCurrentLocationWeather()
                runCurrent()

                assertEquals(1, locationCalls)
                assertEquals("22", viewModel.siteStatus.value.temperature)
                assertEquals(22.0, viewModel.siteStatus.value.recordTemperature)

                weatherResult = ApiResult.Failure(ApiError.Network)
                viewModel.loadCurrentLocationWeather()
                runCurrent()

                assertEquals(2, locationCalls)
                assertEquals("22", viewModel.siteStatus.value.temperature)
                assertEquals(44.0, viewModel.siteStatus.value.recordHumidity)
            } finally {
                siteStatusCollection.cancel()
                advanceTimeBy(5_001L)
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `location weather request ignores response after session generation changes`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val weatherResult = CompletableDeferred<ApiResult<CurrentLocationWeather>>()
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository =
                        FakeTeamSiteRepository(
                            getOverview = { ApiResult.Success(siteOverview("team").copy(currentTemperature = 22.0)) },
                            getWeather = { _, _ -> weatherResult.await() },
                        ),
                    dispatcher = dispatcher,
                    currentLocationProvider = { CurrentCoordinates(37.5665, 126.978) },
                )
            val viewModel = testViewModel.viewModel
            val siteStatusCollection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.siteStatus.collect() }

            try {
                viewModel.refresh()
                runCurrent()
                viewModel.loadCurrentLocationWeather()
                runCurrent()
                testViewModel.sessionManager.onLoginSucceeded(SessionToken("replacement-session-token"))
                weatherResult.complete(ApiResult.Success(locationWeather(WeatherAvailability.AVAILABLE)))
                runCurrent()

                assertEquals(22.0, viewModel.siteStatus.value.recordTemperature)
            } finally {
                siteStatusCollection.cancel()
                advanceTimeBy(5_001L)
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `duplicate location weather request while in flight shares one location lookup`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val locationGate = CompletableDeferred<CurrentCoordinates?>()
            var locationCalls = 0
            val testViewModel =
                createHomeViewModel(
                    recordRepository = FakeRecordHistoryRepository { ApiResult.Success(emptyList()) },
                    siteRepository = FakeTeamSiteRepository { ApiResult.Success(siteOverview("team")) },
                    dispatcher = dispatcher,
                    currentLocationProvider = {
                        locationCalls += 1
                        locationGate.await()
                    },
                )
            val viewModel = testViewModel.viewModel

            try {
                viewModel.loadCurrentLocationWeather()
                viewModel.loadCurrentLocationWeather()
                runCurrent()

                assertEquals(1, locationCalls)
                locationGate.complete(null)
                runCurrent()
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private suspend fun createHomeViewModel(
        recordRepository: RecordHistoryRepository,
        siteRepository: TeamSiteRepository,
        dispatcher: CoroutineDispatcher,
        sessionManager: SessionManager? = null,
        currentLocationProvider: suspend () -> CurrentCoordinates? = { null },
    ): TestHomeViewModel {
        val activeSessionManager = sessionManager ?: createLoggedInSessionManager(dispatcher)
        val viewModel =
            HomeViewModel(
                getTeamSiteOverviewUseCase = GetTeamSiteOverviewUseCase(siteRepository),
                getRecordHistoryUseCase = GetRecordHistoryUseCase(recordRepository),
                sessionManager = activeSessionManager,
                clock = Clock.fixed(java.time.Instant.parse("2026-10-31T03:00:00Z"), ZoneId.of("Asia/Seoul")),
                currentLocationProvider =
                    object : CurrentLocationProvider {
                        override suspend fun getCurrentLocation(): CurrentCoordinates? = currentLocationProvider()
                    },
                getCurrentLocationWeatherUseCase = GetCurrentLocationWeatherUseCase(siteRepository),
            )
        return TestHomeViewModel(viewModel, activeSessionManager)
    }

    private suspend fun createLoggedInSessionManager(dispatcher: CoroutineDispatcher): SessionManager {
        val loggedInSessionManager =
            SessionManager(
                TestTokenStorage(storedAccessToken = null),
                dispatcher,
            )
        loggedInSessionManager.onLoginSucceeded(SessionToken("initial-session-token"))
        return loggedInSessionManager
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
        private val getWeather: suspend (Double, Double) -> ApiResult<CurrentLocationWeather> = { _, _ ->
            error("Weather is not used in this test")
        },
        private val getOverview: suspend () -> ApiResult<TeamSiteOverview>,
    ) : TeamSiteRepository {
        override suspend fun getTeamSiteOverview(): ApiResult<TeamSiteOverview> = getOverview()

        override suspend fun getCurrentLocationWeather(
            latitude: Double,
            longitude: Double,
        ): ApiResult<CurrentLocationWeather> = getWeather(latitude, longitude)
    }

    private fun locationWeather(availability: WeatherAvailability) =
        CurrentLocationWeather(
            temperature = 31.0,
            humidity = 55.0,
            apparentTemperature = 30.8,
            heatLevel = 0,
            observedAt = "2026-11-06T15:00:00+09:00",
            skyStatus = SkyStatus.CLEAR,
            temperatureDelta = 2.1,
            availability = availability,
        )

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

    private class TestTokenStorage(
        private val storedAccessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = storedAccessToken

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
