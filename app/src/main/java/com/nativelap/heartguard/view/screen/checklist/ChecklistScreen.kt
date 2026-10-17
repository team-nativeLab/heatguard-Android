package com.nativelap.heartguard.view.screen.checklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.checklist.ChecklistCompleteButton
import com.nativelap.heartguard.view.component.checklist.ChecklistItemRow
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import com.nativelap.heartguard.viewmodel.checklist.ChecklistItemUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistPeriodStatus
import com.nativelap.heartguard.viewmodel.checklist.ChecklistPeriodUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistUiState

internal sealed interface ChecklistScreenEvent {
    data object BackClicked : ChecklistScreenEvent

    data class ItemToggled(
        val titleResId: Int,
        val isChecked: Boolean,
    ) : ChecklistScreenEvent

    data object CompleteClicked : ChecklistScreenEvent
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChecklistScreen(
    uiState: ChecklistUiState,
    onEvent: (ChecklistScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressFraction =
        if (uiState.items.isEmpty()) {
            0f
        } else {
            uiState.completedCount.toFloat() / uiState.items.size
        }
    val actionTitle =
        if (uiState.isComplete) {
            stringResource(R.string.checklist_complete_action)
        } else {
            stringResource(
                R.string.checklist_complete_action_progress,
                uiState.completedCount,
                uiState.items.size,
            )
        }

    Scaffold(
        modifier = modifier.heartGuardResponsivePage(MaterialTheme.extraColors.pageBackground),
        containerColor = MaterialTheme.extraColors.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                modifier =
                    Modifier.padding(
                        top =
                            HeartGuardSpacing.Section +
                                HeartGuardSpacing.Compact +
                                HeartGuardSpacing.Tight,
                    ),
                title = {
                    Text(
                        text = stringResource(R.string.checklist_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ChecklistScreenEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.checklist_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.extraColors.pageBackground,
                    ),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.extraColors.pageBackground) {
                BottomActionBar {
                    ChecklistCompleteButton(
                        title = actionTitle,
                        enabled = uiState.isComplete,
                        onClick = { onEvent(ChecklistScreenEvent.CompleteClicked) },
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    horizontal = HeartGuardSpacing.HeaderHorizontal,
                    vertical = HeartGuardSpacing.Compact,
                ),
        ) {
            item(key = "checklist-content") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(
                                HeartGuardSpacing.ChecklistSectionGap,
                            ),
                    ) {
                        ChecklistSummaryCard(
                            completedCount = uiState.completedCount,
                            totalCount = uiState.items.size,
                            progress = progressFraction,
                            isComplete = uiState.isComplete,
                        )
                        ChecklistPeriodRow(
                            periods = uiState.periods,
                            isChecklistComplete = uiState.isComplete,
                        )
                        Text(
                            text = stringResource(R.string.checklist_guidance),
                            modifier = Modifier.padding(horizontal = HeartGuardSpacing.Tight),
                            color = MaterialTheme.extraColors.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ChecklistItemsCard(
                            items = uiState.items,
                            onItemToggled = { titleResId, isChecked ->
                                onEvent(
                                    ChecklistScreenEvent.ItemToggled(
                                        titleResId = titleResId,
                                        isChecked = isChecked,
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

@Composable
private fun ChecklistSummaryCard(
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    isComplete: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HeartGuardSpacing.Section,
                    vertical = HeartGuardSpacing.ChecklistSummaryVertical,
                ),
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = stringResource(R.string.checklist_round_title),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Surface(
                shape = RoundedCornerShape(HeartGuardRadius.Pill),
                color =
                    if (isComplete) {
                        MaterialTheme.extraColors.successContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
            ) {
                Text(
                    text =
                        stringResource(
                            if (isComplete) {
                                R.string.checklist_status_complete
                            } else {
                                R.string.checklist_status_in_progress
                            },
                        ),
                    modifier =
                        Modifier.padding(
                            horizontal = HeartGuardSpacing.Compact,
                            vertical = HeartGuardSpacing.Tight,
                        ),
                    color =
                        if (isComplete) {
                            MaterialTheme.extraColors.success
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                )
            }
        }

        Text(
            text = stringResource(R.string.checklist_site_team),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HeartGuardComponentSize.ChecklistProgressHeight)
                    .clip(
                        RoundedCornerShape(
                            HeartGuardComponentSize.ChecklistProgressHeight / 2,
                        ),
                    ),
            color =
                if (isComplete) {
                    MaterialTheme.extraColors.checklistComplete
                } else {
                    MaterialTheme.colorScheme.primary
                },
            trackColor = MaterialTheme.extraColors.homeTimelineTrack,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )

        Text(
            text =
                stringResource(
                    R.string.checklist_progress_format,
                    completedCount,
                    totalCount,
                ),
            color =
                if (isComplete) {
                    MaterialTheme.extraColors.success
                } else {
                    MaterialTheme.colorScheme.primary
                },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChecklistPeriodRow(
    periods: List<ChecklistPeriodUiModel>,
    isChecklistComplete: Boolean,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.ChecklistChipGap),
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.ChecklistChipGap),
    ) {
        periods.forEach { period ->
            val status =
                if (
                    period.status == ChecklistPeriodStatus.CURRENT && isChecklistComplete
                ) {
                    ChecklistPeriodStatus.COMPLETED
                } else {
                    period.status
                }
            ChecklistPeriodChip(
                period = period,
                status = status,
            )
        }
    }
}

@Composable
private fun ChecklistPeriodChip(
    period: ChecklistPeriodUiModel,
    status: ChecklistPeriodStatus,
) {
    val backgroundColor =
        when (status) {
            ChecklistPeriodStatus.COMPLETED,
            ChecklistPeriodStatus.UPCOMING,
            -> MaterialTheme.colorScheme.surface

            ChecklistPeriodStatus.WARNING -> MaterialTheme.extraColors.alertContainer

            ChecklistPeriodStatus.CURRENT -> MaterialTheme.colorScheme.primary
        }
    val contentColor =
        when (status) {
            ChecklistPeriodStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            ChecklistPeriodStatus.WARNING -> MaterialTheme.colorScheme.error
            ChecklistPeriodStatus.CURRENT -> MaterialTheme.colorScheme.onPrimary
            ChecklistPeriodStatus.UPCOMING -> MaterialTheme.extraColors.tertiaryText
        }
    val icon =
        when (status) {
            ChecklistPeriodStatus.COMPLETED -> CHECKLIST_PERIOD_COMPLETED_ICON
            ChecklistPeriodStatus.WARNING -> CHECKLIST_PERIOD_WARNING_ICON
            ChecklistPeriodStatus.CURRENT -> CHECKLIST_PERIOD_CURRENT_ICON
            ChecklistPeriodStatus.UPCOMING -> null
        }
    val statusDescription =
        stringResource(
            when (status) {
                ChecklistPeriodStatus.COMPLETED -> R.string.checklist_item_checked
                ChecklistPeriodStatus.WARNING -> R.string.checklist_period_warning
                ChecklistPeriodStatus.CURRENT -> R.string.checklist_period_current
                ChecklistPeriodStatus.UPCOMING -> R.string.checklist_period_upcoming
            },
        )
    val chipDescription =
        stringResource(
            R.string.checklist_period_accessibility_format,
            period.hour,
            statusDescription,
        )

    Surface(
        modifier =
            Modifier.clearAndSetSemantics {
                contentDescription = chipDescription
            },
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = backgroundColor,
        border =
            when (status) {
                ChecklistPeriodStatus.WARNING -> {
                    BorderStroke(
                        HeartGuardSpacing.Hairline,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    )
                }

                ChecklistPeriodStatus.CURRENT -> {
                    null
                }

                else -> {
                    BorderStroke(
                        HeartGuardSpacing.Hairline,
                        MaterialTheme.extraColors.subtleDivider,
                    )
                }
            },
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.ChecklistChipHorizontal,
                    vertical = HeartGuardSpacing.ChecklistChipVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    color = contentColor,
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
            Text(
                text = stringResource(R.string.checklist_period_hour_format, period.hour),
                color = contentColor,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight =
                            if (status == ChecklistPeriodStatus.CURRENT) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            },
                    ),
            )
        }
    }
}

@Composable
private fun ChecklistItemsCard(
    items: List<ChecklistItemUiModel>,
    onItemToggled: (Int, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.extraColors.checklistDivider,
                )
            }
            ChecklistItemRow(
                title = stringResource(item.titleResId),
                isChecked = item.isChecked,
                onCheckedChange = { isChecked ->
                    onItemToggled(item.titleResId, isChecked)
                },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun ChecklistScreenPreview() {
    HeartGuardTheme {
        ChecklistScreen(
            uiState = ChecklistUiState(),
            onEvent = {},
        )
    }
}

@Preview(
    name = "Compact phone · 200% text",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    fontScale = 2f,
)
@Composable
private fun ChecklistScreenCompactPreview() {
    ChecklistScreenPreview()
}

@Preview(name = "All items complete", showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun ChecklistScreenCompletePreview() {
    HeartGuardTheme {
        val completeUiState =
            ChecklistUiState(
                items =
                    ChecklistUiState().items.map { item ->
                        item.copy(isChecked = true)
                    },
            )
        ChecklistScreen(
            uiState = completeUiState,
            onEvent = {},
        )
    }
}

private const val CHECKLIST_PERIOD_COMPLETED_ICON = "✓"
private const val CHECKLIST_PERIOD_WARNING_ICON = "!"
private const val CHECKLIST_PERIOD_CURRENT_ICON = "●"
