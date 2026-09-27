package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
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
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.HomeTimelineMinHeight),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Tight))

            Text(
                text = nextCheckDescription,
                color = MaterialTheme.extraColors.homeMutedText,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

            HomeTimelineRail(items = items)
        }
    }
}

/** 체크 시점을 점과 시간 라벨로 나열하고, 첫 점부터 마지막 점까지 이어지는 선을 뒤에 그린다.
 * 두 체크 시점이 모두 완료(또는 뒤쪽이 현재 시점)인 구간만 활성 색으로 칠해 Figma의 연속 완료 구간 표현을 따른다. */
@Composable
private fun HomeTimelineRail(
    items: List<CheckTimelineItem>,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.extraColors.homeTimelineTrack
    val activeColor = MaterialTheme.extraColors.homeTimelineActive

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (items.size < 2) {
                    return@drawBehind
                }

                val cellWidth = size.width / items.size
                val railCenterY = HeartGuardIconSize.TimelineCurrentRing.toPx() / 2
                val railStroke = HeartGuardBorderWidth.TimelineRail.toPx()

                fun cellCenterX(index: Int): Float {
                    return cellWidth * (index + 0.5f)
                }

                drawLine(
                    color = trackColor,
                    start = Offset(cellCenterX(0), railCenterY),
                    end = Offset(cellCenterX(items.lastIndex), railCenterY),
                    strokeWidth = railStroke,
                )

                items.zipWithNext().forEachIndexed { segmentIndex, (startItem, endItem) ->
                    val isSegmentActive = startItem.isCompleted &&
                        (endItem.isCompleted || endItem.isCurrent)

                    if (isSegmentActive) {
                        drawLine(
                            color = activeColor,
                            start = Offset(cellCenterX(segmentIndex), railCenterY),
                            end = Offset(cellCenterX(segmentIndex + 1), railCenterY),
                            strokeWidth = railStroke,
                        )
                    }
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        items.forEach { timelineItem ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Box(
                    modifier = Modifier.size(HeartGuardIconSize.TimelineCurrentRing),
                    contentAlignment = Alignment.Center,
                ) {
                    HomeTimelineDot(timelineItem = timelineItem)
                }

                Text(
                    text = timelineItem.timeLabel,
                    color = timelineLabelColor(timelineItem),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = HeartGuardFontSize.TimelineLabel,
                        fontWeight = if (timelineItem.isCurrent) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    ),
                )
            }
        }
    }
}

/** 현재 시점은 옅은 링 안의 점, 완료는 채운 점, 미완료는 흰 속의 테두리 점으로 그린다. */
@Composable
private fun HomeTimelineDot(timelineItem: CheckTimelineItem) {
    when {
        timelineItem.isCurrent -> {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.TimelineCurrentRing)
                    .background(
                        color = MaterialTheme.extraColors.homeTimelineCurrentRing,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(HeartGuardIconSize.TimelineCurrentDot)
                        .background(
                            color = MaterialTheme.extraColors.homeTimelineActive,
                            shape = CircleShape,
                        ),
                )
            }
        }

        timelineItem.isCompleted -> {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.TimelineDot)
                    .background(
                        color = MaterialTheme.extraColors.homeTimelineActive,
                        shape = CircleShape,
                    ),
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.TimelineDot)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                    )
                    .border(
                        width = HeartGuardBorderWidth.TimelineDot,
                        color = MaterialTheme.extraColors.homeTimelineDotBorder,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun timelineLabelColor(timelineItem: CheckTimelineItem): Color {
    return when {
        timelineItem.isCurrent -> MaterialTheme.extraColors.homeTimelineActive
        timelineItem.isCompleted -> MaterialTheme.extraColors.homeMutedText
        else -> MaterialTheme.extraColors.homeTimelineInactive
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
                CheckTimelineItem("14시", true),
                CheckTimelineItem("16시", false),
                CheckTimelineItem("18시", true),
                CheckTimelineItem("20시", true, true),
                CheckTimelineItem("22시", false),
            ),
        )
    }
}
