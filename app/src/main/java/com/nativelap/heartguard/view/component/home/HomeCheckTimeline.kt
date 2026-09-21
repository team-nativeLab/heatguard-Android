package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

@Immutable
data class CheckTimelineItem(
    val timeLabel: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean = false,
)

/** 홈 화면의 오늘 체크 시간을 Figma와 같은 가로 타임라인 카드로 보여준다. */
@Composable
fun HomeCheckTimeline(
    title: String,
    nextCheckDescription: String,
    items: List<CheckTimelineItem>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HeartGuardSpacing.Section,
                    vertical = HeartGuardSpacing.Item,
                ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = nextCheckDescription,
                color = MaterialTheme.extraColors.homeMutedText,
                style = MaterialTheme.typography.bodySmall,
            )
            HomeTimelineRail(
                items = items,
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )
        }
    }
}

/** 체크 상태를 선·점·시간 라벨로 구성하고 현재 체크는 링으로 강조한다. */
@Composable
private fun HomeTimelineRail(
    items: List<CheckTimelineItem>,
    modifier: Modifier = Modifier,
) {
    val activeRailColor = MaterialTheme.extraColors.homeTimelineActive
    val inactiveRailColor = MaterialTheme.extraColors.homeTimelineTrack

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HeartGuardSpacing.LargeSection),
        verticalAlignment = Alignment.Top,
    ) {
        items.forEachIndexed { index, timelineItem ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeartGuardSpacing.Item),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < items.lastIndex) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                            .height(2.dp),
                        ) {
                            drawRect(
                                color = if (timelineItem.isCompleted) {
                                    activeRailColor
                                } else {
                                    inactiveRailColor
                                },
                            )
                        }
                    }
                    if (timelineItem.isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(HeartGuardIconSize.TimelineMarker * 2)
                                .background(
                                    color = MaterialTheme.extraColors.homeMetricContainer,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(HeartGuardIconSize.StatusIndicator)
                                    .background(
                                        color = MaterialTheme.extraColors.homeTimelineActive,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(HeartGuardIconSize.StatusIndicator)
                                .background(
                                    color = if (timelineItem.isCompleted) {
                                        MaterialTheme.extraColors.homeTimelineActive
                                    } else {
                                        MaterialTheme.extraColors.homeTimelineInactive
                                    },
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
                Text(
                    text = timelineItem.timeLabel,
                    color = if (timelineItem.isCurrent) {
                        MaterialTheme.extraColors.homeTimelineActive
                    } else if (timelineItem.isCompleted) {
                        MaterialTheme.extraColors.homeMutedText
                    } else {
                        MaterialTheme.extraColors.homeTimelineInactive
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeCheckTimelinePreview() {
    HeartGuardTheme {
        HomeCheckTimeline(
            title = "오늘 체크 시간",
            nextCheckDescription = "다음 체크까지 57분 · 22:00 예정",
            items = listOf(
                CheckTimelineItem("08시", true),
                CheckTimelineItem("10시", true),
                CheckTimelineItem("12시", false),
                CheckTimelineItem("20시", true, true),
            ),
        )
    }
}
