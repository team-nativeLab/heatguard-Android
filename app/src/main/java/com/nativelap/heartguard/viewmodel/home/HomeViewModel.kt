package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 홈 화면 진입 시 팀 현장페이지(GET /api/v1/t/{teamToken}) 정보를 불러온다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

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
}
