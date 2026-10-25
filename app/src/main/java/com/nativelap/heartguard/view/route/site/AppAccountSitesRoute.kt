package com.nativelap.heartguard.view.route.site

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
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesViewModel

@Composable
internal fun AppAccountSitesRoute(viewModel: AppAccountSitesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var unlinkTarget by remember { mutableStateOf<AppLinkedSite?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    unlinkTarget?.let { site ->
        AlertDialog(
            onDismissRequest = { unlinkTarget = null },
            title = { Text(stringResource(R.string.app_account_site_unlink)) },
            text = { Text(stringResource(R.string.app_account_site_unlink_confirmation, site.siteName)) },
            confirmButton = {
                TextButton(onClick = {
                    unlinkTarget = null
                    viewModel.unlinkSite(site.siteId)
                }) {
                    Text(stringResource(R.string.app_account_site_unlink))
                }
            },
            dismissButton = {
                TextButton(onClick = { unlinkTarget = null }) {
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
                TextButton(onClick = viewModel::logout) {
                    Text(text = stringResource(R.string.auth_logout))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.hasUnlinkFailure) {
                Text(
                    text = stringResource(R.string.app_account_site_unlink_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.hasFailure) {
                Text(
                    text = stringResource(R.string.app_account_sites_error),
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = viewModel::refresh) {
                    Text(text = stringResource(R.string.common_retry))
                }
            } else if (uiState.isLoading) {
                Text(text = stringResource(R.string.app_account_sites_loading))
            } else if (uiState.linkedSites.isEmpty()) {
                Text(text = stringResource(R.string.app_account_sites_empty))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.app_account_sites_link_hint))
            } else {
                Text(text = stringResource(R.string.app_account_sites_description))
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
                                    onClick = { viewModel.selectSite(site.siteId) },
                                    enabled = !uiState.isSelectingSite && !uiState.isUnlinkingSite,
                                    modifier = Modifier.padding(top = 8.dp),
                                ) {
                                    Text(
                                        text =
                                            stringResource(
                                                if (uiState.selectedSiteId == site.siteId) {
                                                    R.string.app_account_site_selected
                                                } else {
                                                    R.string.app_account_site_select
                                                },
                                            ),
                                    )
                                }
                                TextButton(
                                    onClick = { unlinkTarget = site },
                                    enabled = !uiState.isSelectingSite && !uiState.isUnlinkingSite,
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
