package com.nativelap.heartguard.view.route.site

import androidx.compose.runtime.Composable
import com.nativelap.heartguard.view.screen.site.SiteUnlinkConfirmationScreen
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesViewModel

@Composable
internal fun SiteUnlinkConfirmationRoute(
    teamId: String,
    siteId: String,
    onDismiss: () -> Unit,
    viewModel: AppAccountSitesViewModel,
) {
    SiteUnlinkConfirmationScreen(
        onCancelClick = onDismiss,
        onUnlinkClick = {
            viewModel.unlinkTeam(teamId, siteId)
            onDismiss()
        },
    )
}
