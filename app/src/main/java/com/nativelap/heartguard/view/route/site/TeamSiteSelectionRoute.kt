package com.nativelap.heartguard.view.route.site

import androidx.compose.runtime.Composable
import com.nativelap.heartguard.view.screen.site.TeamSiteSelectionScreen
import com.nativelap.heartguard.viewmodel.home.HomeUiState

/** 홈에서 공유하는 작업자 현장 상태만 현장 목록 화면에 전달한다. */
@Composable
internal fun TeamSiteSelectionRoute(
    uiState: HomeUiState,
    onBackClick: () -> Unit,
) {
    val siteOverview = (uiState as? HomeUiState.Success)?.overview
    TeamSiteSelectionScreen(
        siteName = siteOverview?.siteName,
        isLoading = uiState is HomeUiState.Loading,
        hasLoadError = uiState is HomeUiState.Error,
        onBackClick = onBackClick,
    )
}
