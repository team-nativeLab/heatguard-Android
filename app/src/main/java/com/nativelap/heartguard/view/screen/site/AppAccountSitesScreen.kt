package com.nativelap.heartguard.view.screen.site

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesUiState

@Composable
internal fun AppAccountSitesScreen(
    uiState: AppAccountSitesUiState,
    onRefresh: () -> Unit,
    onSelectSite: (String) -> Unit,
    onOpenDashboard: (String) -> Unit,
    isLinkDialogVisible: Boolean,
    linkInput: String,
    hasLinkFailure: Boolean,
    onRequestLink: () -> Unit,
    onLinkInputChange: (String) -> Unit,
    onCancelLink: () -> Unit,
    onOpenLink: () -> Unit,
    unlinkTarget: AppLinkedSite?,
    onRequestUnlink: (AppLinkedSite) -> Unit,
    onCancelUnlink: () -> Unit,
    onConfirmUnlink: () -> Unit,
    onCloseDashboard: () -> Unit,
    onLogout: () -> Unit,
) {
    if (isLinkDialogVisible) {
        AlertDialog(
            onDismissRequest = onCancelLink,
            title = { Text(stringResource(R.string.app_account_add_site)) },
            text = {
                Column {
                    Text(stringResource(R.string.app_account_link_input_hint))
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = onLinkInputChange,
                        label = { Text(stringResource(R.string.app_account_link_input_label)) },
                        singleLine = true,
                        isError = hasLinkFailure,
                    )
                    if (hasLinkFailure) {
                        Text(stringResource(R.string.app_account_link_invalid), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onOpenLink, enabled = linkInput.isNotBlank()) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelLink) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
    unlinkTarget?.let { site ->
        AlertDialog(
            onDismissRequest = onCancelUnlink,
            title = { Text(stringResource(R.string.app_account_site_unlink)) },
            text = { Text(stringResource(R.string.app_account_site_unlink_confirmation, site.siteName)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmUnlink,
                    enabled = !uiState.isLoading && !uiState.isSelectingSite && !uiState.isUnlinkingSite,
                ) {
                    Text(stringResource(R.string.app_account_site_unlink))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelUnlink) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.app_account_sites_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                TextButton(onClick = onLogout) {
                    Text(text = stringResource(R.string.auth_logout))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.openedSiteId != null) {
                TextButton(onClick = onCloseDashboard, enabled = !uiState.isLoadingDashboard) {
                    Text(stringResource(R.string.app_account_sites_switch))
                }
                Text(
                    text =
                        uiState.linkedSites
                            .firstOrNull { it.siteId == uiState.openedSiteId }
                            ?.siteName
                            .orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                )
                when {
                    uiState.isLoading || uiState.isLoadingDashboard -> {
                        Text(stringResource(R.string.app_account_dashboard_loading))
                    }

                    uiState.hasDashboardFailure || uiState.hasFailure -> {
                        Text(
                            stringResource(R.string.app_account_dashboard_error),
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }

                    else -> {
                        uiState.dashboard?.let { dashboard ->
                            Text(stringResource(R.string.app_account_dashboard_records, dashboard.todayRecordCount))
                            Text(
                                stringResource(
                                    R.string.app_account_dashboard_emergencies,
                                    dashboard.activeEmergencyCount,
                                ),
                            )
                        }
                    }
                }
                return@Column
            }

            if (uiState.hasUnlinkFailure) {
                Text(
                    text = stringResource(R.string.app_account_site_unlink_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(onClick = onRequestLink) {
                Text(stringResource(R.string.app_account_add_site))
            }

            if (uiState.hasFailure && uiState.linkedSites.isEmpty()) {
                Text(
                    text = stringResource(R.string.app_account_sites_error),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRefresh) {
                    Text(text = stringResource(R.string.common_retry))
                }
            } else if (uiState.isLoading && uiState.linkedSites.isEmpty()) {
                Text(text = stringResource(R.string.app_account_sites_loading))
            } else if (uiState.linkedSites.isEmpty()) {
                Text(text = stringResource(R.string.app_account_sites_empty))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.app_account_sites_link_hint))
            } else {
                Text(text = stringResource(R.string.app_account_sites_description))
                if (uiState.isLoading) {
                    Text(text = stringResource(R.string.app_account_sites_loading))
                }
                if (uiState.hasFailure) {
                    Text(
                        text = stringResource(R.string.app_account_sites_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onRefresh) {
                        Text(text = stringResource(R.string.common_retry))
                    }
                }
                if (uiState.hasSelectionFailure) {
                    Text(
                        text = stringResource(R.string.app_account_site_selection_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                        items = uiState.linkedSites,
                        key = AppLinkedSite::siteId,
                    ) { site ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = site.siteName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                site.managerName?.takeIf(String::isNotBlank)?.let { managerName ->
                                    Text(text = managerName)
                                }
                                Button(
                                    onClick = { onSelectSite(site.siteId) },
                                    enabled =
                                        !uiState.isSelectingSite && !uiState.isUnlinkingSite &&
                                            !uiState.isLoading && uiState.selectedSiteId != site.siteId,
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Text(
                                        text =
                                            stringResource(
                                                if (uiState.selectingSiteId == site.siteId) {
                                                    R.string.app_account_site_selecting
                                                } else if (uiState.selectedSiteId == site.siteId) {
                                                    R.string.app_account_site_selected
                                                } else {
                                                    R.string.app_account_site_select
                                                },
                                            ),
                                    )
                                }
                                if (uiState.selectedSiteId == site.siteId) {
                                    TextButton(
                                        onClick = { onOpenDashboard(site.siteId) },
                                        enabled =
                                            !uiState.isLoading && !uiState.isLoadingDashboard &&
                                                !uiState.isUnlinkingSite,
                                    ) {
                                        Text(stringResource(R.string.app_account_site_open_dashboard))
                                    }
                                }
                                TextButton(
                                    onClick = { onRequestUnlink(site) },
                                    enabled =
                                        !uiState.isLoading && !uiState.isSelectingSite &&
                                            !uiState.isUnlinkingSite,
                                ) {
                                    Text(stringResource(R.string.app_account_site_unlink))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
