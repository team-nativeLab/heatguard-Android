package com.nativelap.heartguard.view.screen.site

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 현재 선택된 작업자 현장을 보여준다. 현장 목록 조회·전환은 서버 계약이 없어 제공하지 않는다.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TeamSiteSelectionScreen(
    siteName: String?,
    isLoading: Boolean,
    hasLoadError: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.padding(top = TEAM_SITE_SELECTION_HEADER_TOP_OFFSET),
                expandedHeight = TEAM_SITE_SELECTION_HEADER_HEIGHT,
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
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = TEAM_SITE_SELECTION_BACK_BUTTON_START),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.common_back_description),
                            tint = MaterialTheme.extraColors.strongText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.extraColors.pageBackground),
            )
        },
        bottomBar = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(
                            start = HeartGuardSpacing.HeaderHorizontal,
                            top = HeartGuardSpacing.Compact,
                            end = HeartGuardSpacing.HeaderHorizontal,
                            bottom = TEAM_SITE_SELECTION_ADD_BUTTON_BOTTOM_PADDING,
                        ),
            ) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(TEAM_SITE_SELECTION_ADD_BUTTON_HEIGHT),
                    shape = RoundedCornerShape(TEAM_SITE_SELECTION_ADD_BUTTON_RADIUS),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    border =
                        BorderStroke(
                            TEAM_SITE_SELECTION_ADD_BUTTON_BORDER_WIDTH,
                            MaterialTheme.colorScheme.primary.copy(alpha = DISABLED_ACTION_ALPHA),
                        ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = DISABLED_ACTION_ALPHA),
                        modifier = Modifier.size(TEAM_SITE_SELECTION_ADD_ICON_SIZE),
                    )
                    Text(
                        text = stringResource(R.string.team_site_selection_add_link),
                        modifier = Modifier.padding(start = HeartGuardSpacing.Tight),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = DISABLED_ACTION_ALPHA),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = HeartGuardSpacing.HeaderHorizontal,
                        top = TEAM_SITE_SELECTION_CONTENT_TOP_PADDING,
                        end = HeartGuardSpacing.HeaderHorizontal,
                        bottom = HeartGuardSpacing.Item,
                    ),
        ) {
            when {
                isLoading -> {
                    Text(
                        text = stringResource(R.string.team_site_selection_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                hasLoadError || siteName.isNullOrBlank() -> {
                    Text(
                        text = stringResource(R.string.team_site_selection_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    SelectedTeamSiteCard(siteName = siteName)
                }
            }
        }
    }
}

@Composable
private fun SelectedTeamSiteCard(siteName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(TEAM_SITE_SELECTION_CARD_HEIGHT),
        shape = RoundedCornerShape(TEAM_SITE_SELECTION_CARD_RADIUS),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(TEAM_SITE_SELECTION_CARD_BORDER_WIDTH, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TEAM_SITE_SELECTION_CARD_ICON_GAP),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(TEAM_SITE_SELECTION_CHECK_ICON_SIZE),
            )
            Text(
                text = siteName,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.extraColors.strongText,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                modifier = Modifier.width(TEAM_SITE_SELECTION_BADGE_WIDTH).height(TEAM_SITE_SELECTION_BADGE_HEIGHT),
                shape = RoundedCornerShape(TEAM_SITE_SELECTION_BADGE_RADIUS),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.team_site_selection_badge),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    }
}

private val TEAM_SITE_SELECTION_HEADER_TOP_OFFSET = 40.dp
private val TEAM_SITE_SELECTION_HEADER_HEIGHT = 64.dp
private val TEAM_SITE_SELECTION_BACK_BUTTON_START = 8.dp
private val TEAM_SITE_SELECTION_CONTENT_TOP_PADDING = 26.dp
private val TEAM_SITE_SELECTION_CARD_HEIGHT = 92.dp
private val TEAM_SITE_SELECTION_CARD_RADIUS = 14.dp
private val TEAM_SITE_SELECTION_CARD_BORDER_WIDTH = 1.5.dp
private val TEAM_SITE_SELECTION_CARD_ICON_GAP = 12.dp
private val TEAM_SITE_SELECTION_CHECK_ICON_SIZE = 32.dp
private val TEAM_SITE_SELECTION_BADGE_HEIGHT = 24.dp
private val TEAM_SITE_SELECTION_BADGE_WIDTH = 66.dp
private val TEAM_SITE_SELECTION_BADGE_RADIUS = 12.dp
private val TEAM_SITE_SELECTION_ADD_BUTTON_HEIGHT = 54.dp
private val TEAM_SITE_SELECTION_ADD_BUTTON_RADIUS = 16.dp
private val TEAM_SITE_SELECTION_ADD_BUTTON_BORDER_WIDTH = 1.5.dp
private val TEAM_SITE_SELECTION_ADD_ICON_SIZE = 24.dp
private val TEAM_SITE_SELECTION_ADD_BUTTON_BOTTOM_PADDING = 34.dp
private const val DISABLED_ACTION_ALPHA = 0.55f

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun TeamSiteSelectionScreenPreview() {
    HeartGuardTheme {
        TeamSiteSelectionScreen(
            siteName = "이음산업건설 현장",
            isLoading = false,
            hasLoadError = false,
            onBackClick = {},
        )
    }
}
