package com.nativelap.heartguard.view.screen.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.control.HeartGuardCheckbox
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundCardUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundDetailUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundItemUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundsOverviewUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundsUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChecklistRoundsScreen(
    uiState: ChecklistRoundsUiState,
    siteLabel: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRoundClick: (LocalDate, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checklist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.checklist_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = HeartGuardSpacing.HeaderHorizontal),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.rounds?.let { overview ->
                item { ChecklistRoundsSummaryCard(overview, siteLabel) }
                item {
                    Text(
                        stringResource(R.string.checklist_overview_rounds_heading),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(overview.rounds, key = { round -> round.roundId }) { round ->
                    val isLast = round == overview.rounds.lastOrNull()
                    ChecklistRoundCard(
                        round = round,
                        onClick = { onRoundClick(overview.date, round.roundId) },
                        isLast = isLast,
                    )
                }
            }
            if (uiState.isLoadingRounds) item { CircularProgressIndicator() }
            if (uiState.hasRoundsLoadError) {
                item {
                    Button(onClick = onRetryClick) { Text(stringResource(R.string.common_retry)) }
                }
            }
            if (!uiState.isLoadingRounds && !uiState.hasRoundsLoadError && uiState.rounds?.rounds.isNullOrEmpty()) {
                item { Text(stringResource(R.string.checklist_overview_empty)) }
            }
        }
    }
}

@Composable
internal fun ChecklistRoundDetailScreen(
    uiState: ChecklistRoundsUiState,
    date: LocalDate,
    roundId: String,
    siteLabel: String?,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onRetryRoundsClick: () -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
) {
    val detail = uiState.roundDetail?.takeIf { it.date == date && it.roundId == roundId }
    val isComplete = detail != null && detail.totalItems > 0 && detail.checkedItems >= detail.totalItems
    Scaffold(
        containerColor = MaterialTheme.extraColors.pageBackground,
        topBar = {
            WithdrawTopBar(
                title = stringResource(R.string.checklist_title),
                backContentDescription = stringResource(R.string.checklist_back),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (detail != null) {
                BottomActionBar {
                    Button(
                        onClick = onBackClick,
                        enabled = isComplete && uiState.savingItemIds.isEmpty(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text(
                            if (isComplete) {
                                stringResource(R.string.checklist_complete_action)
                            } else {
                                stringResource(
                                    R.string.checklist_complete_action_progress,
                                    detail.checkedItems,
                                    detail.totalItems,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            uiState.isLoadingDetail && detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.hasDetailLoadError && detail == null -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.checklist_load_failure))
                    Button(onClick = onRetryClick) { Text(stringResource(R.string.common_retry)) }
                }
            }

            detail != null -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        ChecklistRoundDetailSummary(
                            detail = detail,
                            siteLabel = siteLabel,
                            rounds =
                                uiState.rounds
                                    ?.takeIf { it.date == date }
                                    ?.rounds
                                    .orEmpty(),
                            onRetryRoundsClick = onRetryRoundsClick,
                            isLoadingRounds = uiState.isLoadingRounds,
                            hasRoundsLoadError = uiState.hasRoundsLoadError,
                            hasSaveError = uiState.hasSaveError,
                        )
                    }
                    if (detail.items.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.checklist_empty),
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.extraColors.secondaryText,
                            )
                        }
                    }
                    if (detail.items.isNotEmpty()) {
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                            ) {
                                detail.items.forEachIndexed { index, row ->
                                    ChecklistRoundItemRow(
                                        item = row,
                                        enabled = detail.canEdit && uiState.savingItemIds.isEmpty() && !isComplete,
                                        showDivider = index < detail.items.lastIndex,
                                        onCheckedChange = { checked -> onItemCheckedChange(row.itemId, checked) },
                                    )
                                }
                            }
                        }
                    }
                    if (!detail.canEdit) {
                        item {
                            Text(
                                stringResource(R.string.checklist_round_read_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extraColors.secondaryText,
                            )
                        }
                    }
                    if (uiState.hasDetailLoadError) {
                        item { TextButton(onClick = onRetryClick) { Text(stringResource(R.string.common_retry)) } }
                    }
                }
            }

            else -> {
                Unit
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChecklistRoundDetailSummary(
    detail: ChecklistRoundDetailUiModel,
    siteLabel: String?,
    rounds: List<ChecklistRoundCardUiModel>,
    onRetryRoundsClick: () -> Unit,
    isLoadingRounds: Boolean,
    hasRoundsLoadError: Boolean,
    hasSaveError: Boolean,
) {
    val allItemsChecked = detail.totalItems > 0 && detail.checkedItems >= detail.totalItems
    val statusLabelResource =
        when (detail.status) {
            ChecklistRoundStatus.COMPLETED -> {
                R.string.checklist_status_complete
            }

            ChecklistRoundStatus.IN_PROGRESS -> {
                if (allItemsChecked) R.string.checklist_status_complete else R.string.checklist_status_in_progress
            }

            ChecklistRoundStatus.MISSED -> {
                R.string.checklist_overview_status_missed
            }

            ChecklistRoundStatus.SCHEDULED -> {
                R.string.checklist_round_status_scheduled
            }

            ChecklistRoundStatus.UNKNOWN -> {
                R.string.checklist_round_status_unknown
            }
        }
    val statusColor =
        when (detail.status) {
            ChecklistRoundStatus.COMPLETED -> {
                MaterialTheme.extraColors.checklistComplete
            }

            ChecklistRoundStatus.IN_PROGRESS -> {
                if (allItemsChecked) MaterialTheme.extraColors.checklistComplete else MaterialTheme.colorScheme.primary
            }

            ChecklistRoundStatus.MISSED -> {
                MaterialTheme.colorScheme.error
            }

            ChecklistRoundStatus.SCHEDULED,
            ChecklistRoundStatus.UNKNOWN,
            -> {
                MaterialTheme.extraColors.secondaryText
            }
        }
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.checklist_selected_period_heading_format,
                        detail.startsAt.hour,
                        detail.startsAt.minute,
                    ),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(statusLabelResource),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            siteLabel?.let {
                Text(it, color = MaterialTheme.extraColors.secondaryText, style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = { detail.progressPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                stringResource(R.string.checklist_progress_format, detail.checkedItems, detail.totalItems),
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (rounds.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 5,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rounds.forEach { round ->
                    val isSelected = round.roundId == detail.roundId
                    ChecklistRoundStatusChip(
                        round = round,
                        isSelected = isSelected,
                        isSelectedComplete =
                            isSelected &&
                                (
                                    detail.status == ChecklistRoundStatus.COMPLETED ||
                                        (detail.status == ChecklistRoundStatus.IN_PROGRESS && allItemsChecked)
                                ),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.checklist_guidance),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
        if (hasSaveError) {
            Text(stringResource(R.string.checklist_round_save_failure), color = MaterialTheme.colorScheme.error)
        }
        if (rounds.isEmpty()) {
            when {
                isLoadingRounds -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }

                hasRoundsLoadError -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.checklist_overview_load_failure),
                            color = MaterialTheme.extraColors.secondaryText,
                        )
                        TextButton(onClick = onRetryRoundsClick) { Text(stringResource(R.string.common_retry)) }
                    }
                }

                else -> {
                    TextButton(onClick = onRetryRoundsClick) { Text(stringResource(R.string.common_retry)) }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRoundStatusChip(
    round: ChecklistRoundCardUiModel,
    isSelected: Boolean,
    isSelectedComplete: Boolean,
) {
    val isCompleted = round.status == ChecklistRoundStatus.COMPLETED
    val isMissed = round.status == ChecklistRoundStatus.MISSED
    val isCurrentSelection = isSelected
    val statusLabel =
        stringResource(
            if (isSelectedComplete) {
                R.string.checklist_period_recorded
            } else {
                when (round.status) {
                    ChecklistRoundStatus.COMPLETED -> R.string.checklist_period_recorded
                    ChecklistRoundStatus.MISSED -> R.string.checklist_period_warning
                    ChecklistRoundStatus.IN_PROGRESS -> R.string.checklist_period_current
                    ChecklistRoundStatus.SCHEDULED -> R.string.checklist_period_upcoming
                    ChecklistRoundStatus.UNKNOWN -> R.string.checklist_round_status_unknown
                }
            },
        )
    val accessibilityDescription =
        stringResource(
            R.string.checklist_period_accessibility_format,
            round.startsAt.hour,
            round.startsAt.minute,
            statusLabel,
        )
    val backgroundColor =
        when {
            isCurrentSelection -> MaterialTheme.colorScheme.primary
            isMissed -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        }
    val foregroundColor =
        when {
            isCurrentSelection -> {
                MaterialTheme.colorScheme.onPrimary
            }

            isMissed -> {
                MaterialTheme.colorScheme.error
            }

            round.status == ChecklistRoundStatus.SCHEDULED || round.status == ChecklistRoundStatus.UNKNOWN -> {
                MaterialTheme.extraColors.strongText.copy(alpha = 0.72f)
            }

            else -> {
                MaterialTheme.extraColors.strongText
            }
        }
    val borderColor =
        when {
            isCurrentSelection -> MaterialTheme.colorScheme.primary
            isMissed -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clearAndSetSemantics { contentDescription = accessibilityDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (isCurrentSelection && !isSelectedComplete) {
            Text("●", color = foregroundColor, style = MaterialTheme.typography.labelSmall)
        } else if (isCompleted || isSelectedComplete) {
            Text(
                "✓",
                color =
                    if (isCurrentSelection) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                style = MaterialTheme.typography.labelSmall,
            )
        } else if (isMissed) {
            Text("!", color = foregroundColor, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            round.startsAt.format(HOUR_FORMATTER),
            color = foregroundColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun ChecklistRoundsSummaryCard(
    overview: ChecklistRoundsOverviewUiModel,
    siteLabel: String?,
) {
    val progressDescription =
        stringResource(
            R.string.checklist_overview_percent_format,
            overview.progressPercent,
        )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
                ),
            ).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(overview.date.toString(), color = Color.White, style = MaterialTheme.typography.titleMedium)
                siteLabel?.let {
                    Text(it, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(
                        R.string.checklist_rounds_completed_summary,
                        overview.completedRounds,
                        overview.totalRounds,
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(start = 12.dp)
                        .size(88.dp)
                        .semantics {
                            contentDescription = progressDescription
                        },
            ) {
                CircularProgressIndicator(
                    progress = { overview.progressPercent.coerceIn(0, 100) / 100f },
                    modifier = Modifier.size(88.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.28f),
                    strokeWidth = 6.dp,
                )
                Text(
                    stringResource(R.string.checklist_overview_percent_format, overview.progressPercent),
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            RoundSummaryStat(overview.completedRounds, stringResource(R.string.checklist_overview_completed_label))
            RoundSummaryStat(overview.missedRounds, stringResource(R.string.checklist_overview_missed_label))
            RoundSummaryStat(overview.remainingRounds, stringResource(R.string.checklist_overview_remaining_label))
        }
    }
}

@Composable
private fun RoundSummaryStat(
    count: Int,
    label: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun ChecklistRoundCard(
    round: ChecklistRoundCardUiModel,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    val statusLabel =
        when (round.status) {
            ChecklistRoundStatus.COMPLETED -> {
                stringResource(R.string.checklist_overview_badge_completed)
            }

            ChecklistRoundStatus.MISSED -> {
                stringResource(R.string.checklist_overview_status_missed)
            }

            ChecklistRoundStatus.IN_PROGRESS -> {
                stringResource(R.string.checklist_round_status_in_progress)
            }

            ChecklistRoundStatus.SCHEDULED -> {
                stringResource(R.string.checklist_round_status_scheduled)
            }

            ChecklistRoundStatus.UNKNOWN -> {
                stringResource(R.string.checklist_round_status_unknown)
            }
        }
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ChecklistTimelineRail(status = round.status, isLast = isLast)
        Card(
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (round.status == ChecklistRoundStatus.IN_PROGRESS) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    ).clickable(role = Role.Button, onClick = onClick)
                    .semantics {
                        contentDescription = "$statusLabel, ${round.startsAt.format(TIME_FORMATTER)}"
                    },
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        when (round.status) {
                            ChecklistRoundStatus.COMPLETED -> MaterialTheme.extraColors.successContainer
                            ChecklistRoundStatus.MISSED -> MaterialTheme.extraColors.alertContainer
                            ChecklistRoundStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(round.startsAt.format(TIME_FORMATTER), style = MaterialTheme.typography.titleMedium)
                    Text(
                        statusLabel,
                        color =
                            when (round.status) {
                                ChecklistRoundStatus.COMPLETED,
                                ChecklistRoundStatus.IN_PROGRESS,
                                -> MaterialTheme.colorScheme.primary

                                ChecklistRoundStatus.MISSED -> MaterialTheme.colorScheme.error

                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                Text(
                    stringResource(
                        R.string.checklist_overview_items_completed_format,
                        round.checkedItems,
                        round.totalItems,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text =
                        round.completedAt?.let { completedAt ->
                            stringResource(
                                R.string.checklist_round_completed_at_format,
                                completedAt.format(DATE_TIME_FORMATTER),
                            )
                        } ?: stringResource(
                            R.string.checklist_round_ends_at_format,
                            round.endsAt.format(DATE_TIME_FORMATTER),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ChecklistTimelineRail(
    status: ChecklistRoundStatus,
    isLast: Boolean,
) {
    val dotColor =
        when (status) {
            ChecklistRoundStatus.COMPLETED -> MaterialTheme.extraColors.checklistComplete
            ChecklistRoundStatus.MISSED -> MaterialTheme.colorScheme.error
            ChecklistRoundStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
        }
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .width(TIMELINE_RAIL_WIDTH)
                .fillMaxHeight()
                .drawBehind {
                    if (!isLast) {
                        val lineStartY = TIMELINE_DOT_TOP.toPx() + TIMELINE_DOT_SIZE.toPx()
                        val lineEndY = size.height + ITEM_VERTICAL_GAP.toPx() + TIMELINE_DOT_TOP.toPx()
                        drawLine(
                            color = trackColor,
                            start = Offset(x = size.width / 2f, y = lineStartY),
                            end = Offset(x = size.width / 2f, y = lineEndY),
                            strokeWidth = TIMELINE_LINE_WIDTH.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = TIMELINE_DOT_TOP)
                .size(TIMELINE_DOT_SIZE)
                .background(dotColor, RoundedCornerShape(50)),
        )
    }
}

@Composable
internal fun ChecklistRoundItemRow(
    item: ChecklistRoundItemUiModel,
    enabled: Boolean,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val dividerColor = MaterialTheme.extraColors.checklistDivider
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 55.dp)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (showDivider) {
                    Modifier.drawBehind {
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                },
            ).toggleable(
                value = item.isChecked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartGuardCheckbox(
            isChecked = item.isChecked,
            onCheckedChange = {},
            contentDescription = item.title,
            isEnabled = enabled,
            isInteractive = false,
            isCircular = true,
        )
        Text(
            item.title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color =
                if (item.isChecked) {
                    MaterialTheme.extraColors.checklistCompletedText
                } else {
                    MaterialTheme.extraColors.strongText
                },
        )
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH'시'", Locale.KOREAN)
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
private val TIMELINE_RAIL_WIDTH = 20.dp
private val TIMELINE_DOT_SIZE = 12.dp
private val TIMELINE_DOT_TOP = 18.dp
private val TIMELINE_LINE_WIDTH = 2.dp
private val ITEM_VERTICAL_GAP = 12.dp
