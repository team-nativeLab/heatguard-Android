package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.history.RecordHistoryCounts
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter

/** 조회 기간의 유형별 기록 건수 요약 카드다(Figma 20). 건수는 서버에서 받은 기록을 센 값이다. */
@Composable
fun RecordHistoryCountSummaryCard(
    recordCounts: RecordHistoryCounts,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(vertical = HeartGuardSpacing.Card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecordCountColumn(
                title = recordHistoryFilterText(RecordHistoryFilter.ALL),
                recordCount = recordCounts.total,
                modifier = Modifier.weight(1f),
            )
            CountDivider()
            RecordCountColumn(
                title = recordHistoryFilterText(RecordHistoryFilter.THERMOMETER),
                recordCount = recordCounts.thermometer,
                modifier = Modifier.weight(1f),
            )
            CountDivider()
            RecordCountColumn(
                title = recordHistoryFilterText(RecordHistoryFilter.WORK),
                recordCount = recordCounts.work,
                modifier = Modifier.weight(1f),
            )
            CountDivider()
            RecordCountColumn(
                title = recordHistoryFilterText(RecordHistoryFilter.REST),
                recordCount = recordCounts.rest,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecordCountColumn(
    title: String,
    recordCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
    ) {
        Text(
            text = title,
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = stringResource(R.string.history_count_format, recordCount),
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun CountDivider() {
    VerticalDivider(
        modifier = Modifier.height(HeartGuardSpacing.LargeSection),
        thickness = HeartGuardBorderWidth.Divider,
        color = MaterialTheme.extraColors.cardBorder,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryCountSummaryCardPreview() {
    HeartGuardTheme {
        RecordHistoryCountSummaryCard(
            recordCounts = RecordHistoryCounts(
                total = 25,
                thermometer = 12,
                work = 8,
                rest = 5,
            ),
        )
    }
}
