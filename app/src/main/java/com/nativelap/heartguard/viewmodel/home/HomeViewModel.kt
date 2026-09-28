package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** 홈 화면 진입 시 팀 현장페이지(GET /api/v1/t/{teamToken}) 정보를 불러온다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadTeamSiteOverview()

        viewModelScope.launch {
            sessionManager.sessionState.drop(1).collect { currentSessionState ->
                when (currentSessionState) {
                    SessionState.Authenticated -> loadTeamSiteOverview()
                    SessionState.Unauthenticated,
                    SessionState.Initializing -> reset()
                }
            }
        }
    }

    fun loadTeamSiteOverview() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.value = HomeUiState.Loading
            mutableUiState.value = when (val result = getTeamSiteOverviewUseCase()) {
                is ApiResult.Success -> HomeUiState.Success(result.value)
                is ApiResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }

    /** 로그아웃·세션 만료 시 이전 작업자의 홈 데이터를 지우고 진행 중인 요청을 취소한다. */
    fun reset() {
        loadJob?.cancel()
        loadJob = null
        mutableUiState.value = HomeUiState.Loading
    }
}
