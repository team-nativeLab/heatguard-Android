package com.nativelap.heartguard.view.route.site

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.screen.site.AppAccountSitesScreen
import com.nativelap.heartguard.view.screen.site.AppAccountSitesScreenEvent
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesViewModel

@Composable
internal fun AppAccountSitesRoute(
    onBackClick: () -> Unit,
    viewModel: AppAccountSitesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    AppAccountSitesScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                AppAccountSitesScreenEvent.BackClicked -> {
                    onBackClick()
                }

                AppAccountSitesScreenEvent.RetryClicked -> {
                    viewModel.refresh()
                }

                is AppAccountSitesScreenEvent.SiteClicked -> {
                    if (uiState.selectedSiteId == event.siteId) {
                        viewModel.unlinkSite(event.siteId)
                    } else {
                        viewModel.selectSite(event.siteId)
                    }
                }
            }
        },
    )
}
