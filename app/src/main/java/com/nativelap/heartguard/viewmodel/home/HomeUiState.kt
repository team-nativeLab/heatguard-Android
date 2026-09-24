package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(val overview: TeamSiteOverview) : HomeUiState

    data class Error(val error: ApiError) : HomeUiState
}
