package com.nativelap.heartguard.view.screen.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.extraColors.pageBackground,
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = HeartGuardSpacing.HeaderHorizontal,
                    top = CHECKLIST_OVERVIEW_CONTENT_TOP_PADDING,
                    end = HeartGuardSpacing.HeaderHorizontal,
                    bottom = HeartGuardSpacing.Compact,
                ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            if (schedule != null) {
                item(key = "overview-summary") {
                    ChecklistOverviewSummary(
                        completedCount = completedCount,
                        warningCount = periods.count { period -> period.status == PeriodStatus.WARNING },
                        totalCount = periods.size,
                        siteLabel = siteLabel,
                        date = overviewDate,
                    )
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
                        Column(
                            modifier = Modifier.padding(top = CHECKLIST_OVERVIEW_HEADING_TOP_PADDING),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.checklist_overview_rounds_heading),
                                    color = MaterialTheme.extraColors.strongText,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = stringResource(R.string.checklist_overview_interval),
                                    color = MaterialTheme.extraColors.strongText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(modifier = Modifier.height(CHECKLIST_OVERVIEW_HEADING_ROW_GAP))
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
    val (statusLabel, labelColor, badgeColor) =
        when (period.status) {
            PeriodStatus.COMPLETED -> {
                Triple(
                    stringResource(R.string.checklist_overview_badge_completed),
                    MaterialTheme.extraColors.success,
                    Color(0xFFE8F7EE),
                )
            }

            PeriodStatus.WARNING -> {
                Triple(
                    stringResource(R.string.checklist_overview_status_missed),
                    MaterialTheme.colorScheme.error,
                    Color(0xFFFFF1F0),
                )
            }

            PeriodStatus.CURRENT -> {
                Triple(
                    stringResource(R.string.checklist_overview_badge_current),
                    MaterialTheme.colorScheme.primary,
                    Color(0xFFEAF2FD),
                )
            }

            PeriodStatus.UPCOMING -> {
                Triple(
                    stringResource(R.string.checklist_overview_badge_upcoming),
                    MaterialTheme.extraColors.strongText,
                    Color(0xFFF1F3F6),
                )
            }
        }
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(CHECKLIST_TIMELINE_CARD_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(CHECKLIST_TIMELINE_COLUMN_WIDTH)
                    .heightIn(min = CHECKLIST_TIMELINE_MIN_ROW_HEIGHT)
                    .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(CHECKLIST_TIMELINE_LINE_WIDTH)
                        .fillMaxHeight()
                        .background(CHECKLIST_TIMELINE_COLOR),
            )
            Box(
                modifier =
                    Modifier
                        .size(CHECKLIST_TIMELINE_DOT_SIZE)
                        .shadow(CHECKLIST_TIMELINE_DOT_ELEVATION, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(statusColor(period.status)),
                contentAlignment = Alignment.Center,
            ) {
                when (period.status) {
                    PeriodStatus.COMPLETED -> {
                        Icon(
                            Icons.Default.Check,
                            null,
                            modifier = Modifier.size(CHECKLIST_TIMELINE_ICON_SIZE),
                            tint = Color.White,
                        )
                    }

                    PeriodStatus.WARNING -> {
                        Text(
                            text = stringResource(R.string.checklist_overview_timeline_warning),
                            color = Color.White,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontSize = CHECKLIST_TIMELINE_WARNING_SIZE,
                                    fontWeight = FontWeight.Bold,
                                ),
                        )
                    }

                    PeriodStatus.CURRENT -> {
                        Box(
                            Modifier
                                .size(
                                    CHECKLIST_TIMELINE_CENTER_SIZE,
                                ).clip(RoundedCornerShape(50))
                                .background(Color.White),
                        )
                    }

                    PeriodStatus.UPCOMING -> {
                        Unit
                    }
                }
            }
        }
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = CHECKLIST_TIMELINE_MIN_ROW_HEIGHT)
                    .shadow(
                        elevation =
                            if (period.status == PeriodStatus.CURRENT) {
                                CHECKLIST_CURRENT_CARD_ELEVATION
                            } else {
                                CHECKLIST_ROUND_CARD_ELEVATION
                            },
                        shape = RoundedCornerShape(CHECKLIST_ROUND_CARD_RADIUS),
                    ).clickable(
                        onClickLabel = stringResource(R.string.checklist_overview_open_detail),
                        role = Role.Button,
                        onClick = onClick,
                    ),
            shape = RoundedCornerShape(CHECKLIST_ROUND_CARD_RADIUS),
            color = MaterialTheme.colorScheme.surface,
            border =
                if (period.status == PeriodStatus.CURRENT) {
                    androidx.compose.foundation.BorderStroke(CHECKLIST_CURRENT_BORDER_WIDTH, labelColor)
                } else {
                    null
                },
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(
                        start = CHECKLIST_ROUND_CARD_START_PADDING,
                        top = CHECKLIST_PERIOD_VERTICAL_PADDING,
                        end = CHECKLIST_ROUND_CARD_END_PADDING,
                        bottom = CHECKLIST_PERIOD_VERTICAL_PADDING,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CHECKLIST_ROUND_CARD_CONTENT_GAP),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CHECKLIST_ROUND_TEXT_GAP),
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.checklist_period_time_format,
                                period.time.hour,
                                period.time.minute,
                            ),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = CHECKLIST_ROUND_TITLE_SIZE,
                                lineHeight = CHECKLIST_ROUND_TITLE_LINE_HEIGHT,
                                fontWeight = FontWeight.Bold,
                            ),
                        color =
                            MaterialTheme.extraColors.strongText,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(HeartGuardRadius.Pill),
                    color = badgeColor,
                ) {
                    Text(
                        text = statusLabel,
                        modifier =
                            Modifier.padding(
                                horizontal = CHECKLIST_STATUS_BADGE_HORIZONTAL,
                                vertical = CHECKLIST_STATUS_BADGE_VERTICAL,
                            ),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = CHECKLIST_STATUS_BADGE_SIZE,
                                fontWeight = FontWeight.Bold,
                            ),
                        color = labelColor,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.extraColors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun statusColor(status: PeriodStatus): Color =
    when (status) {
        PeriodStatus.COMPLETED -> MaterialTheme.extraColors.success
        PeriodStatus.WARNING -> MaterialTheme.colorScheme.error
        PeriodStatus.CURRENT -> MaterialTheme.colorScheme.primary
        PeriodStatus.UPCOMING -> MaterialTheme.extraColors.secondaryText
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
    val largeText = LocalDensity.current.fontScale > CHECKLIST_OVERVIEW_LARGE_FONT_SCALE

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CHECKLIST_OVERVIEW_CARD_HEIGHT)
                .shadow(CHECKLIST_OVERVIEW_CARD_ELEVATION, RoundedCornerShape(CHECKLIST_OVERVIEW_RADIUS))
                .clip(RoundedCornerShape(CHECKLIST_OVERVIEW_RADIUS))
                .background(Brush.linearGradient(CHECKLIST_OVERVIEW_GRADIENT)),
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = CHECKLIST_DECORATION_LARGE_OFFSET_X, y = CHECKLIST_DECORATION_LARGE_OFFSET_Y)
                        .size(CHECKLIST_DECORATION_LARGE_SIZE)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f)),
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = CHECKLIST_DECORATION_SMALL_OFFSET_X, y = CHECKLIST_DECORATION_SMALL_OFFSET_Y)
                        .size(CHECKLIST_DECORATION_SMALL_SIZE)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.06f)),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CHECKLIST_OVERVIEW_CONTENT_HORIZONTAL_PADDING,
                        top = CHECKLIST_OVERVIEW_CONTENT_TOP_INSET,
                        end = CHECKLIST_OVERVIEW_CONTENT_HORIZONTAL_PADDING,
                        bottom = HeartGuardSpacing.Section,
                    ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            if (largeText) {
                Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
                    Text(
                        text =
                            listOfNotNull(
                                date.format(CHECKLIST_OVERVIEW_DATE_FORMATTER),
                                siteLabel?.takeIf(String::isNotBlank),
                            ).joinToString(CHECKLIST_SUMMARY_SEPARATOR),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = MUTED_TEXT_ALPHA),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = CHECKLIST_SUMMARY_DATE_SIZE),
                    )
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
                                text = completedCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = CHECKLIST_SUMMARY_COUNT_SIZE,
                                        fontWeight = FontWeight.Bold,
                                    ),
                            )
                            Text(
                                text = stringResource(R.string.checklist_overview_completed_of_format, totalCount),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        OverviewProgressIndicator(
                            progress = progress,
                            progressPercent = progressPercent,
                        )
                    }
                }
            } else {
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
                            text =
                                listOfNotNull(
                                    date.format(CHECKLIST_OVERVIEW_DATE_FORMATTER),
                                    siteLabel?.takeIf(String::isNotBlank),
                                ).joinToString(CHECKLIST_SUMMARY_SEPARATOR),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = MUTED_TEXT_ALPHA),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = CHECKLIST_SUMMARY_DATE_SIZE),
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = completedCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = CHECKLIST_SUMMARY_COUNT_SIZE,
                                        fontWeight = FontWeight.Bold,
                                    ),
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
                    OverviewProgressIndicator(
                        progress = progress,
                        progressPercent = progressPercent,
                    )
                }
            }
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = CHECKLIST_OVERVIEW_STATS_HEIGHT)
                        .offset(y = if (largeText) 0.dp else CHECKLIST_OVERVIEW_STATS_OFFSET),
                shape = RoundedCornerShape(CHECKLIST_OVERVIEW_STATS_RADIUS),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = SUMMARY_STATS_ALPHA),
            ) {
                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = HeartGuardSpacing.Compact,
                            vertical = HeartGuardSpacing.Tight,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OverviewSummaryStat(
                        modifier = Modifier.weight(1f),
                        count = completedCount,
                        label = stringResource(R.string.checklist_overview_completed_label),
                    )
                    OverviewStatDivider()
                    OverviewSummaryStat(
                        modifier = Modifier.weight(1f),
                        count = warningCount,
                        label = stringResource(R.string.checklist_overview_missed_label),
                    )
                    OverviewStatDivider()
                    OverviewSummaryStat(
                        modifier = Modifier.weight(1f),
                        count = remainingCount,
                        label = stringResource(R.string.checklist_overview_remaining_label),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewProgressIndicator(
    progress: Float,
    progressPercent: Int,
) {
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

@Composable
private fun OverviewSummaryStat(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontSize = CHECKLIST_OVERVIEW_STAT_COUNT_SIZE,
                    fontWeight = FontWeight.Bold,
                ),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = CHECKLIST_OVERVIEW_STAT_LABEL_SIZE),
        )
    }
}

@Composable
private fun OverviewStatDivider() {
    Box(
        modifier =
            Modifier
                .height(CHECKLIST_OVERVIEW_STATS_DIVIDER_HEIGHT)
                .width(CHECKLIST_OVERVIEW_STATS_DIVIDER_WIDTH)
                .background(Color.White.copy(alpha = CHECKLIST_OVERVIEW_STATS_DIVIDER_ALPHA)),
    )
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
            schedule =
                CheckSchedule(
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

private val CHECKLIST_OVERVIEW_RING_SIZE = 64.dp
private val CHECKLIST_OVERVIEW_RING_STROKE = 6.dp
private const val PERCENT_SCALE = 100
private const val MUTED_TEXT_ALPHA = 0.82f
private const val RING_TRACK_ALPHA = 0.28f
private const val SUMMARY_STATS_ALPHA = 0.16f
private val CHECKLIST_TIMELINE_DOT_SIZE = 22.dp
private val CHECKLIST_TIMELINE_COLUMN_WIDTH = 22.dp
private val CHECKLIST_TIMELINE_LINE_WIDTH = 2.dp
private val CHECKLIST_TIMELINE_CENTER_SIZE = 6.dp
private val CHECKLIST_TIMELINE_ICON_SIZE = 14.dp
private val CHECKLIST_TIMELINE_WARNING_SIZE = 12.sp
private val CHECKLIST_TIMELINE_DOT_ELEVATION = 1.dp
private val CHECKLIST_CURRENT_BORDER_WIDTH = 1.dp
private val CHECKLIST_PERIOD_VERTICAL_PADDING = 12.dp
private val CHECKLIST_TIMELINE_MIN_ROW_HEIGHT = 58.dp
private val CHECKLIST_TIMELINE_COLOR = Color(0xFFDFE5EE)
private val CHECKLIST_OVERVIEW_HEADING_ROW_GAP = 8.dp
private val CHECKLIST_OVERVIEW_HEADING_TOP_PADDING = 9.dp
private val CHECKLIST_ROUND_TEXT_GAP = 2.dp
private val CHECKLIST_TIMELINE_CARD_GAP = 12.dp
private val CHECKLIST_ROUND_CARD_RADIUS = 14.dp
private val CHECKLIST_ROUND_CARD_ELEVATION = 2.dp
private val CHECKLIST_CURRENT_CARD_ELEVATION = 6.dp
private val CHECKLIST_ROUND_CARD_START_PADDING = 16.dp
private val CHECKLIST_ROUND_CARD_END_PADDING = 12.dp
private val CHECKLIST_ROUND_CARD_CONTENT_GAP = 10.dp
private val CHECKLIST_STATUS_BADGE_HORIZONTAL = 9.dp
private val CHECKLIST_STATUS_BADGE_VERTICAL = 3.dp
private val CHECKLIST_STATUS_BADGE_SIZE = 11.sp
private val CHECKLIST_ROUND_TITLE_SIZE = 15.sp
private val CHECKLIST_ROUND_TITLE_LINE_HEIGHT = 18.sp
private val CHECKLIST_OVERVIEW_CARD_ELEVATION = 10.dp
private val CHECKLIST_OVERVIEW_RADIUS = 22.dp
private val CHECKLIST_OVERVIEW_CARD_HEIGHT = 168.dp
private val CHECKLIST_OVERVIEW_CONTENT_HORIZONTAL_PADDING = 22.dp
private val CHECKLIST_OVERVIEW_CONTENT_TOP_INSET = 12.dp
private val CHECKLIST_OVERVIEW_STATS_OFFSET = 18.dp
private val CHECKLIST_OVERVIEW_STATS_HEIGHT = 54.dp
private const val CHECKLIST_OVERVIEW_LARGE_FONT_SCALE = 1.3f
private val CHECKLIST_OVERVIEW_STATS_RADIUS = 14.dp
private val CHECKLIST_OVERVIEW_STATS_DIVIDER_HEIGHT = 28.dp
private val CHECKLIST_OVERVIEW_STATS_DIVIDER_WIDTH = 1.dp
private const val CHECKLIST_OVERVIEW_STATS_DIVIDER_ALPHA = 0.20f
private val CHECKLIST_OVERVIEW_GRADIENT = listOf(Color(0xFF3F93F7), Color(0xFF1C62D6))
private val CHECKLIST_DECORATION_LARGE_SIZE = 180.dp
private val CHECKLIST_DECORATION_LARGE_OFFSET_X = 56.dp
private val CHECKLIST_DECORATION_LARGE_OFFSET_Y = (-60).dp
private val CHECKLIST_DECORATION_SMALL_SIZE = 120.dp
private val CHECKLIST_DECORATION_SMALL_OFFSET_X = 46.dp
private val CHECKLIST_DECORATION_SMALL_OFFSET_Y = 42.dp
private val CHECKLIST_SUMMARY_DATE_SIZE = 12.sp
private val CHECKLIST_SUMMARY_COUNT_SIZE = 34.sp
private val CHECKLIST_OVERVIEW_STAT_COUNT_SIZE = 16.sp
private val CHECKLIST_OVERVIEW_STAT_LABEL_SIZE = 11.sp
private const val CHECKLIST_SUMMARY_SEPARATOR = " · "
private val CHECKLIST_OVERVIEW_CONTENT_TOP_PADDING = 15.dp
private const val CHECKLIST_OVERVIEW_DATE_POLL_MILLIS = 30_000L
private val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private val CHECKLIST_OVERVIEW_DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
