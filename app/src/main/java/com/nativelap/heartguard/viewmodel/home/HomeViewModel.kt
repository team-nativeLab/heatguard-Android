package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.domain.site.model.buildCheckSchedule
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 작업자 홈(GET /api/v1/team) 정보를 불러와 홈과 기록·긴급 화면에 함께 제공한다.
 * HeartGuardMainNavDisplay에서 한 번 만들어 필요한 Route에 명시적으로 넘긴다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
    private val getRecordHistoryUseCase: GetRecordHistoryUseCase,
    private val sessionManager: SessionManager,
    private val clock: Clock,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    // 오늘 저장한 기록의 측정 시각(현지 시각)이다. 체크 타임라인 완료 표시에 쓰며, 받지 못했으면 비어 있다.
    private val todayRecordTimes = MutableStateFlow<List<LocalTime>>(emptyList())

    /** 여러 화면이 공통으로 쓰는 현장 값. 응답이 없으면 모든 값이 null이다. */
    val siteStatus: StateFlow<SiteStatusUiModel> = uiState
        .map { homeUiState -> homeUiState.toSiteStatusUiModel() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SiteStatusUiModel(),
        )

    // 화면이 떠 있는 동안 1분마다 현재 시각을 흘려보내 "다음 체크까지 N분" 문구와 현재 체크 표시를 갱신한다.
    private val currentTime = flow {
        while (true) {
            emit(LocalTime.now(clock))
            delay(TIME_TICK_MILLIS)
        }
    }

    /** 서버 체크 시각·오늘 기록·현재 시각으로 계산한 오늘의 체크 일정. 홈 응답이 없으면 null이다. */
    val checkSchedule: StateFlow<CheckSchedule?> = combine(
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
        refresh()
    }

    /** 홈으로 돌아올 때(화면 재개·기록 저장 후) 호출한다. 이미 받은 값은 새 응답이 올 때까지 그대로 보여 깜빡이지 않게 하고,
     * 새로 받은 결과로 바꾼다. 이전 값이 있는데 새 조회만 실패하면 이전 값을 유지하고 [HomeUiState.Error]로 바꾸지 않는다. */
    fun refresh() {
        loadJob?.cancel()
        val owningGeneration = sessionManager.getSnapshot().generation
        loadJob = viewModelScope.launch {
            val fetchedRecordTimes = fetchTodayRecordTimes()
            val overviewResult = getTeamSiteOverviewUseCase()
            // 두 조회 사이에 계정이 바뀌었으면 이전 계정 응답을 새 계정 홈에 반영하지 않는다.
            if (owningGeneration != sessionManager.getSnapshot().generation) {
                return@launch
            }
            fetchedRecordTimes?.let { recordTimes -> todayRecordTimes.value = recordTimes }
            val currentState = mutableUiState.value
            mutableUiState.value = when (overviewResult) {
                is ApiResult.Success -> HomeUiState.Success(overviewResult.value)
                is ApiResult.Failure -> if (currentState is HomeUiState.Success) {
                    currentState
                } else {
                    HomeUiState.Error(overviewResult.error)
                }
            }
            mutableRefreshCompletionCount.value += 1
        }
    }

    /** 세션이 바뀌거나 메인 흐름을 벗어날 때 이전 팀의 응답과 진행 중인 요청을 제거한다. */
    fun reset() {
        loadJob?.cancel()
        loadJob = null
        mutableUiState.value = HomeUiState.Loading
        todayRecordTimes.value = emptyList()
    }

    // 오늘(Asia/Seoul) 기록의 측정 시각을 받는다. 실패하면 null을 돌려 이전 값을 유지하게 한다(완료 표시를 임의로 지우지 않는다).
    private suspend fun fetchTodayRecordTimes(): List<LocalTime>? {
        val today = LocalDate.now(clock)
        val todayRecordsResult = getRecordHistoryUseCase(
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
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TIME_TICK_MILLIS = 60_000L
    }
}
