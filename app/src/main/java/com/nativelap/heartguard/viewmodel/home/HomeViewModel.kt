package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.nativelap.heartguard.core.location.CurrentLocationProvider
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.domain.site.model.CurrentLocationWeather
import com.nativelap.heartguard.domain.site.model.WeatherAvailability
import com.nativelap.heartguard.domain.site.model.WeatherUnavailableReason
import com.nativelap.heartguard.domain.site.model.buildCheckSchedule
import com.nativelap.heartguard.domain.site.usecase.GetCurrentLocationWeatherUseCase
import com.nativelap.heartguard.domain.site.usecase.GetCachedCurrentLocationWeatherUseCase
import com.nativelap.heartguard.domain.site.usecase.PruneExpiredCurrentLocationWeatherCacheUseCase
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration
import javax.inject.Inject

/** 작업자 홈(GET /api/v1/team) 정보를 불러와 홈과 기록·긴급 화면에 함께 제공한다.
 * HeartGuardMainNavDisplay에서 한 번 만들어 필요한 Route에 명시적으로 넘긴다. */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
        private val getRecordHistoryUseCase: GetRecordHistoryUseCase,
        private val sessionManager: SessionManager,
        private val clock: Clock,
        private val currentLocationProvider: CurrentLocationProvider,
        private val getCurrentLocationWeatherUseCase: GetCurrentLocationWeatherUseCase,
        private val getCachedCurrentLocationWeatherUseCase: GetCachedCurrentLocationWeatherUseCase,
        private val pruneExpiredCurrentLocationWeatherCacheUseCase: PruneExpiredCurrentLocationWeatherCacheUseCase,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

        // 오늘 저장한 기록의 측정 시각(현지 시각)이다. 체크 타임라인 완료 표시에 쓰며, 받지 못했으면 비어 있다.
        private val todayRecordTimes = MutableStateFlow<List<LocalTime>>(emptyList())

        private val currentLocationWeather = MutableStateFlow<CurrentLocationWeather?>(null)
        private val mutableLocationWeatherStatus =
            MutableStateFlow<LocationWeatherStatus>(LocationWeatherStatus.Idle)
        val locationWeatherStatus: StateFlow<LocationWeatherStatus> = mutableLocationWeatherStatus.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        private val weatherFreshnessTime =
            combine(uiState, currentLocationWeather) { homeUiState, weather ->
                homeUiState to weather
            }.transformLatest { (homeUiState, weather) ->
                while (true) {
                    val now = clock.instant()
                    emit(now)
                    try {
                        pruneExpiredCurrentLocationWeatherCacheUseCase(now)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (exception: Exception) {
                        Log.w(TAG, "Unable to prune expired weather cache", exception)
                    }
                    val nextRefresh = nextWeatherRefreshAt(homeUiState, weather, now) ?: return@transformLatest
                    delay(Duration.between(now, nextRefresh).toMillis().coerceAtLeast(1L))
                }
            }

        /** 여러 화면이 공통으로 쓰는 현장 값. 응답이 없으면 모든 값이 null이다. */
        val siteStatus: StateFlow<SiteStatusUiModel> =
            combine(uiState, currentLocationWeather, weatherFreshnessTime) { homeUiState, weather, now ->
                homeUiState.toSiteStatusUiModel(weather, now)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SiteStatusUiModel(),
            )

        private val currentTime =
            flow {
                while (true) {
                    emit(LocalTime.now(clock))
                    delay(TIME_TICK_MILLIS)
                }
            }

        // 화면이 떠 있는 동안 1분마다 현재 시각을 흘려보내 "다음 체크까지 N분" 문구와 현재 체크 표시를 갱신한다.
        /** 서버 체크 시각·오늘 기록·현재 시각으로 계산한 오늘의 체크 일정. 홈 응답이 없으면 null이다. */
        val checkSchedule: StateFlow<CheckSchedule?> =
            combine(
                uiState,
                todayRecordTimes,
                currentTime,
            ) { homeUiState, recordTimes, now ->
                (homeUiState as? HomeUiState.Success)
                    ?.overview
                    ?.checkTimes
                    ?.let { rawCheckTimes ->
                        buildCheckSchedule(
                            rawCheckTimes = rawCheckTimes,
                            now = now,
                            todayRecordTimes = recordTimes,
                        )
                    }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

        private var loadJob: Job? = null
        private var currentLocationWeatherJob: Job? = null
        private var currentLocationWeatherGeneration: Long? = null
        private var currentLocationWeatherRequestId = 0L

        // 진행 중인 조회를 시작한 세션 세대다. 계정이 바뀐 뒤 남은 이전 조회 때문에 새 조회를 건너뛰지 않게 한다.
        private var loadJobGeneration: Long? = null

        // 홈 조회가 끝날 때마다 늘어난다. 응답 내용이 같아도 진행 중 긴급호출 이어받기를 다시 시도하는 기준으로 쓴다.
        private val mutableRefreshCompletionCount = MutableStateFlow(0)
        val refreshCompletionCount: StateFlow<Int> = mutableRefreshCompletionCount.asStateFlow()

        init {
            clearStateWhenSessionEnds(sessionManager) {
                reset()
                // 다른 계정으로 바로 다시 로그인해 홈이 그대로 떠 있으면 새 계정 값을 이어서 조회한다.
                if (sessionManager.sessionState.value == SessionState.Authenticated) {
                    loadTeamSiteOverview()
                }
            }
        }

        /** 홈 정보와 오늘 기록을 조회한다. 로그인 직후처럼 이전 값이 없을 때 쓰며, 응답 전에는 모든 값을 "--"로 둔다. */
        fun loadTeamSiteOverview() {
            mutableUiState.value = HomeUiState.Loading
            startLoad(shouldRestartRunningLoad = true)
        }

        /** 메인 흐름이 처음 구성되거나 회전으로 다시 구성될 때 호출한다. 받은 값이 있거나 같은 세션의 조회가 진행 중이면 그대로 둔다. */
        fun loadTeamSiteOverviewIfNeeded() {
            if (mutableUiState.value is HomeUiState.Success || isLoadRunningForCurrentSession()) {
                return
            }
            mutableUiState.value = HomeUiState.Loading
            startLoad(shouldRestartRunningLoad = false)
        }

        /** 권한을 승인한 경우에만 홈에서 호출해 현재 좌표 날씨를 한 번 조회한다. */
        fun loadCurrentLocationWeather() {
            val generation = sessionManager.getSnapshot().generation
            if (currentLocationWeatherJob?.isActive == true &&
                currentLocationWeatherGeneration == generation
            ) {
                return
            }
            currentLocationWeatherJob?.cancel()
            mutableLocationWeatherStatus.value = LocationWeatherStatus.CheckingLocation
            currentLocationWeatherGeneration = generation
            val requestId = ++currentLocationWeatherRequestId
            currentLocationWeatherJob =
                viewModelScope.launch {
                    var coordinates =
                        currentLocationProvider
                            .getCurrentLocation()
                            ?.takeIf { currentCoordinates -> currentCoordinates.isValid }
                    if (coordinates == null) {
                        delay(LOCATION_RETRY_DELAY_MILLIS)
                        ensureActive()
                        if (requestId != currentLocationWeatherRequestId ||
                            generation != sessionManager.getSnapshot().generation
                        ) {
                            return@launch
                        }
                        coordinates =
                            currentLocationProvider
                                .getCurrentLocation()
                                ?.takeIf { currentCoordinates -> currentCoordinates.isValid }
                    }
                    if (coordinates == null) {
                        currentLocationWeather.value = null
                        mutableLocationWeatherStatus.value = LocationWeatherStatus.LocationUnavailable
                        return@launch
                    }
                    if (requestId != currentLocationWeatherRequestId ||
                        generation != sessionManager.getSnapshot().generation
                    ) {
                        return@launch
                    }
                    mutableLocationWeatherStatus.value = LocationWeatherStatus.RequestingWeather
                    val result =
                        getCurrentLocationWeatherUseCase(
                            coordinates.latitude,
                            coordinates.longitude,
                        )
                    if (requestId != currentLocationWeatherRequestId ||
                        generation != sessionManager.getSnapshot().generation
                    ) {
                        return@launch
                    }
                    when (result) {
                        is ApiResult.Failure -> {
                            currentLocationWeather.value =
                                getCachedCurrentLocationWeatherUseCase(coordinates.latitude, coordinates.longitude)
                            mutableLocationWeatherStatus.value = LocationWeatherStatus.WeatherRequestFailed
                        }

                        is ApiResult.Success -> {
                            if (result.value.availability == WeatherAvailability.AVAILABLE ||
                                result.value.availability == WeatherAvailability.CACHED
                            ) {
                                currentLocationWeather.value = result.value
                                mutableLocationWeatherStatus.value = LocationWeatherStatus.Available
                            } else {
                                currentLocationWeather.value =
                                    getCachedCurrentLocationWeatherUseCase(coordinates.latitude, coordinates.longitude)
                                mutableLocationWeatherStatus.value =
                                    LocationWeatherStatus.WeatherUnavailable(
                                        result.value.unavailableReason ?: WeatherUnavailableReason.Unknown,
                                    )
                            }
                        }
                    }
                }
        }

        fun refreshCurrentLocationWeather() = loadCurrentLocationWeather()

        private fun nextWeatherRefreshAt(
            homeUiState: HomeUiState,
            locationWeather: CurrentLocationWeather?,
            now: java.time.Instant,
        ): java.time.Instant? {
            val teamOverview = (homeUiState as? HomeUiState.Success)?.overview
            return nextWeatherTimestampRefresh(
                now,
                listOfNotNull(
                    locationWeather?.observedAt,
                    locationWeather?.coldWaveWarning?.fetchedAt,
                    teamOverview?.observedAt,
                    teamOverview?.coldWaveWarning?.fetchedAt,
                )
            )
        }

        /** 위치 권한이 없거나 철회된 경우 진행 중 요청과 위치 날씨 override를 제거한다. */
        fun clearCurrentLocationWeather() {
            currentLocationWeatherRequestId += 1
            currentLocationWeatherJob?.cancel()
            currentLocationWeatherJob = null
            currentLocationWeatherGeneration = null
            currentLocationWeather.value = null
            mutableLocationWeatherStatus.value = LocationWeatherStatus.Idle
        }

        fun markLocationPermissionDenied() {
            currentLocationWeatherRequestId += 1
            currentLocationWeatherJob?.cancel()
            currentLocationWeatherJob = null
            currentLocationWeatherGeneration = null
            currentLocationWeather.value = null
            mutableLocationWeatherStatus.value = LocationWeatherStatus.PermissionDenied
        }

        /** 자정 이후에는 전날 완료 기록을 제거한 뒤 오늘 일정과 기록을 다시 불러온다. */
        fun refreshForNewDay() {
            todayRecordTimes.value = emptyList()
            loadTeamSiteOverview()
        }

        /** 홈으로 돌아올 때(화면 재개·기록 저장 후) 호출한다. 이미 받은 값은 새 응답이 올 때까지 그대로 보여 깜빡이지 않게 하고,
         * 새로 받은 결과로 바꾼다. 이전 값이 있는데 새 조회만 실패하면 이전 값을 유지하고 [HomeUiState.Error]로 바꾸지 않는다.
         * 같은 세션의 조회가 이미 진행 중이면 취소하지 않고 그 결과를 기다린다. */
        fun refresh() {
            startLoad(shouldRestartRunningLoad = false)
        }

        private fun isLoadRunningForCurrentSession(): Boolean =
            loadJob?.isActive == true &&
                loadJobGeneration == sessionManager.getSnapshot().generation

        private fun startLoad(shouldRestartRunningLoad: Boolean) {
            if (!shouldRestartRunningLoad && isLoadRunningForCurrentSession()) {
                return
            }
            loadJob?.cancel()
            val owningGeneration = sessionManager.getSnapshot().generation
            loadJobGeneration = owningGeneration
            loadJob =
                viewModelScope.launch {
                    val (fetchedRecordTimes, overviewResult) =
                        coroutineScope {
                            val recordTimes = async { fetchTodayRecordTimes() }
                            val overview = async { getTeamSiteOverviewUseCase() }
                            recordTimes.await() to overview.await()
                        }
                    // 조회가 진행되는 동안 계정이 바뀌었으면 이전 계정 응답을 새 계정 홈에 반영하지 않는다.
                    if (owningGeneration != sessionManager.getSnapshot().generation) {
                        return@launch
                    }
                    fetchedRecordTimes?.let { recordTimes -> todayRecordTimes.value = recordTimes }
                    val currentState = mutableUiState.value
                    mutableUiState.value =
                        when (overviewResult) {
                            is ApiResult.Success -> {
                                HomeUiState.Success(overviewResult.value)
                            }

                            is ApiResult.Failure -> {
                                if (currentState is HomeUiState.Success) {
                                    currentState
                                } else {
                                    HomeUiState.Error(overviewResult.error)
                                }
                            }
                        }
                    mutableRefreshCompletionCount.value += 1
                }
        }

        /** 세션이 바뀌거나 메인 흐름을 벗어날 때 이전 팀의 응답과 진행 중인 요청을 제거한다. */
        fun reset() {
            loadJob?.cancel()
            clearCurrentLocationWeather()
            loadJob = null
            loadJobGeneration = null
            mutableUiState.value = HomeUiState.Loading
            todayRecordTimes.value = emptyList()
        }

        // 오늘(Asia/Seoul) 기록의 측정 시각을 받는다. 실패하면 null을 돌려 이전 값을 유지하게 한다(완료 표시를 임의로 지우지 않는다).
        private suspend fun fetchTodayRecordTimes(): List<LocalTime>? {
            val today = LocalDate.now(clock)
            val todayRecordsResult =
                getRecordHistoryUseCase(
                    startDate = today,
                    endDate = today,
                )
            if (todayRecordsResult !is ApiResult.Success) {
                return null
            }
            return todayRecordsResult.value.mapNotNull { recordEntry ->
                recordEntry.measuredAt
                    ?.atZoneSameInstant(clock.zone)
                    ?.toLocalTime()
            }
        }

        private companion object {
            const val TAG = "HomeViewModel"
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val TIME_TICK_MILLIS = 60_000L
            const val LOCATION_RETRY_DELAY_MILLIS = 1_000L
        }
    }

sealed interface LocationWeatherStatus {
    data object Idle : LocationWeatherStatus

    data object CheckingLocation : LocationWeatherStatus

    data object RequestingWeather : LocationWeatherStatus

    data object LocationUnavailable : LocationWeatherStatus

    data object WeatherRequestFailed : LocationWeatherStatus

    data class WeatherUnavailable(
        val reason: WeatherUnavailableReason,
    ) : LocationWeatherStatus

    data object Available : LocationWeatherStatus

    data object PermissionDenied : LocationWeatherStatus
}
