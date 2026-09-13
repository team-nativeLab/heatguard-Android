package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

@Immutable
data class CheckTimelineItem(
    val timeLabel: String,
    val title: String,
    val isCompleted: Boolean,
)

/** 홈 화면의 오늘 점검 기록을 세로 타임라인으로 보여준다. */
@Composable
fun HomeCheckTimeline(
    title: String,
    items: List<CheckTimelineItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        items.forEachIndexed { index, timelineItem ->
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(vertical = HeartGuardSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .width(HeartGuardIconSize.Information)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(HeartGuardIconSize.TimelineMarker)
                            .background(
                                color = if (timelineItem.isCompleted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape,
                            ),
                    )
                    if (index < items.lastIndex) {
                        Spacer(
                            modifier = Modifier
                                .width(HeartGuardBorderWidth.TimelineRail)
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(start = HeartGuardSpacing.Item),
                ) {
                    Text(
                        text = timelineItem.timeLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = timelineItem.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeCheckTimelinePreview() {
    HeartGuardTheme {
        HomeCheckTimeline(
            title = "오늘의 점검",
            items = listOf(
                CheckTimelineItem("09:00", "아침 온도 기록", true),
                CheckTimelineItem("13:00", "오후 온도 기록", false),
            ),
        )
    }
}
