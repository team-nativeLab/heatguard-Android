package com.nativelap.heartguard.view.screen.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundCardUiModel
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
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChecklistRoundDetailScreen(
    uiState: ChecklistRoundsUiState,
    date: LocalDate,
    roundId: String,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
) {
    val detail = uiState.roundDetail?.takeIf { it.date == date && it.roundId == roundId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(roundId) },
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
        when {
            uiState.isLoadingDetail && detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.hasDetailLoadError && detail == null -> {
                Button(onClick = onRetryClick, modifier = Modifier.padding(padding)) {
                    Text(stringResource(R.string.common_retry))
                }
            }

            detail != null -> {
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(
                                    R.string.checklist_overview_items_completed_format,
                                    detail.checkedItems,
                                    detail.totalItems,
                                ),
                            )
                            LinearProgressIndicator(
                                progress = { detail.progressPercent.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (!detail.canEdit) {
                                Text(
                                    stringResource(R.string.checklist_round_read_only),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (uiState.hasSaveError) {
                                Text(
                                    stringResource(R.string.checklist_round_save_failure),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    items(detail.items, key = { row -> row.itemId }) { row ->
                        ChecklistRoundItemRow(
                            item = row,
                            enabled = detail.canEdit && uiState.savingItemIds.isEmpty(),
                            onCheckedChange = { checked -> onItemCheckedChange(row.itemId, checked) },
                        )
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
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onCheckedChange(!item.isChecked)
            }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isChecked, enabled = enabled, onCheckedChange = onCheckedChange)
        Text(item.title, modifier = Modifier.weight(1f))
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)
private val TIMELINE_RAIL_WIDTH = 20.dp
private val TIMELINE_DOT_SIZE = 12.dp
private val TIMELINE_DOT_TOP = 18.dp
private val TIMELINE_LINE_WIDTH = 2.dp
private val ITEM_VERTICAL_GAP = 12.dp
