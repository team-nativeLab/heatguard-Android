package com.nativelap.heartguard.view.route.site

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.view.screen.site.AppAccountSitesScreen
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesViewModel

@Composable
internal fun AppAccountSitesRoute(
    onOpenLink: (String) -> Boolean,
    viewModel: AppAccountSitesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var unlinkTarget by remember { mutableStateOf<AppLinkedSite?>(null) }
    var isLinkDialogVisible by remember { mutableStateOf(false) }
    var linkInput by remember { mutableStateOf("") }
    var hasLinkFailure by remember { mutableStateOf(false) }
    BackHandler(enabled = uiState.openedSiteId != null) {
        viewModel.closeDashboard()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    AppAccountSitesScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onSelectSite = viewModel::selectSite,
        isLinkDialogVisible = isLinkDialogVisible,
        linkInput = linkInput,
        hasLinkFailure = hasLinkFailure,
        onRequestLink = {
            isLinkDialogVisible = true
            linkInput = ""
            hasLinkFailure = false
        },
        onLinkInputChange = {
            linkInput = it
            hasLinkFailure = false
        },
        onCancelLink = {
            isLinkDialogVisible = false
            linkInput = ""
        },
        onOpenLink = {
            if (onOpenLink(linkInput)) {
                isLinkDialogVisible = false
                linkInput = ""
            } else {
                hasLinkFailure = true
            }
        },
        unlinkTarget = unlinkTarget,
        onRequestUnlink = { unlinkTarget = it },
        onCancelUnlink = { unlinkTarget = null },
        onConfirmUnlink = {
            unlinkTarget?.let { viewModel.unlinkSite(it.siteId) }
            unlinkTarget = null
        },
        onCloseDashboard = viewModel::closeDashboard,
        onLogout = viewModel::logout,
    )
}
