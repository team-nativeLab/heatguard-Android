package com.nativelap.heartguard.view.screen.site

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.AppLinkedSite
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppAccountSitesScreen(
    uiState: AppAccountSitesUiState,
    onEvent: (AppAccountSitesScreenEvent) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.extraColors.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = SITE_SELECTION_HEADER_HEIGHT,
                title = {
                    Text(
                        text = stringResource(R.string.team_site_selection_title),
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(AppAccountSitesScreenEvent.BackClicked) },
                        modifier = Modifier.padding(start = SITE_SELECTION_BACK_BUTTON_START),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.common_back_description),
                            tint = MaterialTheme.extraColors.strongText,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.extraColors.pageBackground,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = SITE_SELECTION_HORIZONTAL_PADDING),
        ) {
            when {
                uiState.isLoading && uiState.linkedSites.isEmpty() -> {
                    CenteredStatusMessage(
                        message = stringResource(R.string.team_site_selection_loading),
                    )
                }

                uiState.hasFailure && uiState.linkedSites.isEmpty() -> {
                    CenteredStatusMessage(
                        message = stringResource(R.string.app_account_sites_error),
                        color = MaterialTheme.colorScheme.error,
                        onRetryClick = { onEvent(AppAccountSitesScreenEvent.RetryClicked) },
                    )
                }

                uiState.linkedSites.isEmpty() -> {
                    CenteredStatusMessage(
                        message = stringResource(R.string.team_site_selection_empty),
                    )
                }

                else -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(top = SITE_SELECTION_CONTENT_TOP_PADDING),
                        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                    ) {
                        if (uiState.hasFailure) {
                            Text(
                                text = stringResource(R.string.app_account_sites_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = { onEvent(AppAccountSitesScreenEvent.RetryClicked) }) {
                                Text(text = stringResource(R.string.common_retry))
                            }
                        }
                        if (uiState.hasSelectionFailure) {
                            Text(
                                text = stringResource(R.string.app_account_site_selection_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (uiState.hasUnlinkFailure) {
                            Text(
                                text = stringResource(R.string.app_account_site_unlink_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(SITE_SELECTION_CARD_GAP),
                        ) {
                            items(
                                items = uiState.linkedSites,
                                key = AppLinkedSite::teamId,
                            ) { site ->
                                SiteSelectionCard(
                                    site = site,
                                    isSelected = uiState.selectedTeamId == site.teamId,
                                    isSelecting = uiState.selectingTeamId == site.teamId,
                                    isUnlinking = uiState.unlinkingTeamId == site.teamId,
                                    isEnabled =
                                        !uiState.isSelectingSite &&
                                            !uiState.isUnlinkingSite &&
                                            !uiState.isLoading &&
                                            !uiState.isLoadingDashboard,
                                    onClick = {
                                        onEvent(
                                            AppAccountSitesScreenEvent.TeamClicked(
                                                teamId = site.teamId,
                                                siteId = site.siteId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredStatusMessage(
    message: String,
    color: Color = MaterialTheme.extraColors.secondaryText,
    onRetryClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = message,
                color = color,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (onRetryClick != null) {
                Button(onClick = onRetryClick) {
                    Text(text = stringResource(R.string.common_retry))
                }
            }
        }
    }
}

@Composable
private fun SiteSelectionCard(
    site: AppLinkedSite,
    isSelected: Boolean,
    isSelecting: Boolean,
    isUnlinking: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val cardColor =
        if (isSelected) {
            MaterialTheme.extraColors.siteSelectedContainer
        } else {
            MaterialTheme.extraColors.siteUnselectedContainer
        }
    val borderColor =
        if (isSelected) {
            MaterialTheme.extraColors.siteSelectedBorder
        } else {
            MaterialTheme.extraColors.siteUnselectedBorder
        }
    val labelColor =
        if (isSelected) {
            MaterialTheme.extraColors.siteSelectedLabel
        } else {
            MaterialTheme.extraColors.siteUnselectedText
        }
    val siteNameColor =
        if (isSelected) {
            MaterialTheme.extraColors.strongText
        } else {
            MaterialTheme.extraColors.siteUnselectedText
        }
    val statusLabel =
        stringResource(
            when {
                isUnlinking -> R.string.app_account_site_unlinking
                isSelecting -> R.string.app_account_site_selecting
                isSelected -> R.string.app_account_site_selected
                else -> R.string.team_site_selection_unselected_badge
            },
        )
    val actionDescription =
        stringResource(
            when {
                isUnlinking -> R.string.app_account_site_unlinking_action_description
                isSelected -> R.string.app_account_site_selected_action_description
                else -> R.string.app_account_site_unselected_action_description
            },
            site.teamName,
            site.siteName,
        )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = SITE_SELECTION_CARD_HEIGHT)
                .semantics(mergeDescendants = true) {
                    contentDescription = actionDescription
                    selected = isSelected
                }.clickable(
                    enabled = isEnabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        shape = RoundedCornerShape(SITE_SELECTION_CARD_RADIUS),
        color = cardColor,
        border =
            BorderStroke(
                if (isSelected) SITE_SELECTION_SELECTED_BORDER_WIDTH else SITE_SELECTION_BORDER_WIDTH,
                borderColor,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = SITE_SELECTION_CARD_HORIZONTAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.extraColors.siteSelectedBorder,
                    modifier = Modifier.size(SITE_SELECTION_SELECTED_ICON_SIZE),
                )
            } else {
                Spacer(modifier = Modifier.size(SITE_SELECTION_SELECTED_ICON_SIZE))
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = SITE_SELECTION_CARD_CONTENT_GAP),
            ) {
                Text(
                    text = site.siteName,
                    color = siteNameColor,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = site.teamName,
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = statusLabel,
                color = labelColor,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
        }
    }
}

private val SITE_SELECTION_HEADER_HEIGHT = 64.dp
private val SITE_SELECTION_BACK_BUTTON_START = 8.dp
private val SITE_SELECTION_HORIZONTAL_PADDING = 24.dp
private val SITE_SELECTION_CONTENT_TOP_PADDING = 26.dp
private val SITE_SELECTION_CARD_HEIGHT = 92.dp
private val SITE_SELECTION_CARD_RADIUS = 14.dp
private val SITE_SELECTION_CARD_GAP = 10.dp
private val SITE_SELECTION_CARD_HORIZONTAL_PADDING = 16.dp
private val SITE_SELECTION_CARD_CONTENT_GAP = 12.dp
private val SITE_SELECTION_SELECTED_ICON_SIZE = 32.dp
private val SITE_SELECTION_SELECTED_BORDER_WIDTH = 1.5.dp
private val SITE_SELECTION_BORDER_WIDTH = 1.dp
