package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.domain.site.model.buildCheckSchedule
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
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

/** 팀 현장페이지(GET /api/v1/t/{teamToken}) 정보를 불러와 홈과 기록·긴급 화면에 함께 제공한다.
 * HeartGuardMainNavDisplay에서 한 번 만들어 필요한 Route에 명시적으로 넘긴다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

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

    /** 서버 체크 시각과 현재 시각으로 계산한 오늘의 체크 일정. 응답이 없으면 null이다. */
    val checkSchedule: StateFlow<CheckSchedule?> = combine(uiState, currentTime) { homeUiState, now ->
        (homeUiState as? HomeUiState.Success)
            ?.overview
            ?.checkTimes
            ?.let { rawCheckTimes ->
                buildCheckSchedule(
                    rawCheckTimes = rawCheckTimes,
                    now = now,
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
    )

    init {
        loadTeamSiteOverview()
    }

    fun loadTeamSiteOverview() {
        viewModelScope.launch {
            mutableUiState.value = HomeUiState.Loading
            mutableUiState.value = when (val result = getTeamSiteOverviewUseCase()) {
                is ApiResult.Success -> HomeUiState.Success(result.value)
                is ApiResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TIME_TICK_MILLIS = 60_000L
    }
}
