package com.nativelap.heartguard.view.screen.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import java.time.LocalTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private enum class PeriodStatus {
    COMPLETED,
    WARNING,
    CURRENT,
    UPCOMING,
}

private data class OverviewPeriod(
    val time: LocalTime,
    val status: PeriodStatus,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ChecklistOverviewScreen(
    schedule: CheckSchedule?,
    siteLabel: String?,
    isLoading: Boolean,
    hasLoadError: Boolean,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPeriodClick: (LocalTime) -> Unit,
    onNewDay: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var overviewDate by remember { mutableStateOf(LocalDate.now(KOREA_ZONE)) }
    val periods = schedule.toOverviewPeriods()
    val completedCount = periods.count { period -> period.status == PeriodStatus.COMPLETED }

    LaunchedEffect(Unit) {
        while (true) {
            delay(CHECKLIST_OVERVIEW_DATE_POLL_MILLIS)
            val currentDate = LocalDate.now(KOREA_ZONE)
            if (currentDate != overviewDate) {
                overviewDate = currentDate
                onNewDay()
            }
        }
    }

    Scaffold(
        modifier = modifier.heartGuardResponsivePage(MaterialTheme.extraColors.pageBackground),
        containerColor = MaterialTheme.extraColors.pageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.checklist_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.checklist_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.extraColors.pageBackground,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = HeartGuardSpacing.HeaderHorizontal,
                vertical = HeartGuardSpacing.Compact,
            ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            if (schedule != null) {
                item(key = "overview-summary") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(HeartGuardRadius.Card),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        ChecklistOverviewSummary(
                            completedCount = completedCount,
                            warningCount = periods.count { period -> period.status == PeriodStatus.WARNING },
                            totalCount = periods.size,
                            siteLabel = siteLabel,
                        date = overviewDate,
                        )
                    }
                }
            }

            when {
                isLoading && schedule == null -> {
                    item(key = "overview-loading") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(HeartGuardSpacing.Section),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.checklist_overview_loading),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                hasLoadError && schedule == null -> {
                    item(key = "overview-error") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(HeartGuardSpacing.Section),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                        ) {
                            Text(
                                text = stringResource(R.string.checklist_overview_load_failure),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = onRetryClick) {
                                Text(text = stringResource(R.string.common_retry))
                            }
                        }
                    }
                }

                schedule == null || periods.isEmpty() -> {
                    item(key = "overview-empty") {
                        Text(
                            text = stringResource(R.string.checklist_overview_empty),
                            modifier = Modifier.fillMaxWidth().padding(HeartGuardSpacing.Section),
                            color = MaterialTheme.extraColors.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                else -> {
                    item(key = "overview-guidance") {
                        Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
                            Text(
                                text = stringResource(R.string.checklist_overview_rounds_heading),
                                color = MaterialTheme.extraColors.strongText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = stringResource(R.string.checklist_overview_guidance),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    items(periods, key = { period -> period.time.toString() }) { period ->
                        OverviewPeriodCard(
                            period = period,
                            onClick = { onPeriodClick(period.time) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewPeriodCard(
    period: OverviewPeriod,
    onClick: () -> Unit,
) {
    val (statusLabel, containerColor, labelColor) =
        when (period.status) {
            PeriodStatus.COMPLETED -> Triple(
                stringResource(R.string.checklist_period_recorded),
                MaterialTheme.colorScheme.surface,
                MaterialTheme.extraColors.success,
            )
            PeriodStatus.WARNING -> Triple(
                stringResource(R.string.checklist_overview_status_missed),
                MaterialTheme.extraColors.alertContainer,
                MaterialTheme.colorScheme.error,
            )
            PeriodStatus.CURRENT -> Triple(
                stringResource(R.string.checklist_period_current),
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary,
            )
            PeriodStatus.UPCOMING -> Triple(
                stringResource(R.string.checklist_period_upcoming),
                MaterialTheme.colorScheme.surface,
                MaterialTheme.extraColors.tertiaryText,
            )
        }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            onClickLabel = stringResource(R.string.checklist_overview_open_detail),
            role = Role.Button,
            onClick = onClick,
        ),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(HeartGuardSpacing.Section),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Text(
                    text = stringResource(
                        R.string.checklist_period_time_format,
                        period.time.hour,
                        period.time.minute,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = labelColor,
                )
            }
            Text(
                text = stringResource(R.string.common_chevron_right),
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ChecklistOverviewSummary(
    completedCount: Int,
    warningCount: Int,
    totalCount: Int,
    siteLabel: String?,
    date: LocalDate,
) {
    val remainingCount = (totalCount - completedCount - warningCount).coerceAtLeast(0)
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val progressPercent = (progress * PERCENT_SCALE).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Text(
                        text = date.format(CHECKLIST_OVERVIEW_DATE_FORMATTER),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = MUTED_TEXT_ALPHA),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    siteLabel?.takeIf(String::isNotBlank)?.let { label ->
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = MUTED_TEXT_ALPHA),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = completedCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = stringResource(R.string.checklist_overview_completed_of_format, totalCount),
                        modifier =
                            Modifier.padding(
                                start = HeartGuardSpacing.Tight,
                                bottom = HeartGuardSpacing.Tight,
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Box(
                    modifier = Modifier.size(CHECKLIST_OVERVIEW_RING_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = RING_TRACK_ALPHA),
                        strokeWidth = CHECKLIST_OVERVIEW_RING_STROKE,
                    )
                    Text(
                        text = stringResource(R.string.checklist_overview_percent_format, progressPercent),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HeartGuardRadius.Card),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = SUMMARY_STATS_ALPHA),
            ) {
                Row(
                    modifier = Modifier.padding(HeartGuardSpacing.Compact),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OverviewSummaryStat(
                        count = completedCount,
                        label = stringResource(R.string.checklist_overview_completed_label),
                    )
                    OverviewSummaryStat(
                        count = warningCount,
                        label = stringResource(R.string.checklist_overview_missed_label),
                    )
                    OverviewSummaryStat(
                        count = remainingCount,
                        label = stringResource(R.string.checklist_overview_remaining_label),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewSummaryStat(
    count: Int,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun CheckSchedule?.toOverviewPeriods(): List<OverviewPeriod> {
    val schedule = this ?: return emptyList()
    return schedule.checkTimes.map { checkTime ->
        val status =
            when {
                checkTime in schedule.completedCheckTimes -> PeriodStatus.COMPLETED
                checkTime == schedule.nextCheckTime -> PeriodStatus.CURRENT
                schedule.nextCheckTime == null || checkTime.isBefore(schedule.nextCheckTime) -> PeriodStatus.WARNING
                else -> PeriodStatus.UPCOMING
            }
        OverviewPeriod(time = checkTime, status = status)
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistOverviewScreenPreview() {
    HeartGuardTheme {
        ChecklistOverviewScreen(
            schedule = CheckSchedule(
                checkTimes = listOf(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0)),
                nextCheckTime = LocalTime.of(12, 0),
                minutesUntilNextCheck = 20,
                completedCheckTimes = setOf(LocalTime.of(8, 0)),
            ),
            siteLabel = "",
            isLoading = false,
            hasLoadError = false,
            onBackClick = {},
            onRetryClick = {},
            onPeriodClick = {},
        )
    }
}

private val CHECKLIST_OVERVIEW_RING_SIZE = 72.dp
private val CHECKLIST_OVERVIEW_RING_STROKE = 6.dp
private const val PERCENT_SCALE = 100
private const val MUTED_TEXT_ALPHA = 0.82f
private const val RING_TRACK_ALPHA = 0.28f
private const val SUMMARY_STATS_ALPHA = 0.16f
private const val CHECKLIST_OVERVIEW_DATE_POLL_MILLIS = 30_000L
private val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val CHECKLIST_OVERVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
