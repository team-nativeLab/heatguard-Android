package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.history.RecordHistoryCounts
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter

/** 조회 기간의 유형별 기록 건수 요약 카드다(Figma 20). 건수는 지금까지 받은 기록을 센 값이며,
 * 아직 받을 기록이 남았으면([hasMoreRecords]) 건수 뒤에 '+'를 붙여 기간 전체 건수가 아님을 드러낸다. */
@Composable
fun RecordHistoryCountSummaryCard(
    recordCounts: RecordHistoryCounts,
    modifier: Modifier = Modifier,
    hasMoreRecords: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // 좁은 화면·큰 글꼴에서는 네 칸을 한 줄에 두면 라벨과 건수가 쪼개지므로 두 줄(2×2)로 나눈다.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val countColumns = listOf(
                RecordHistoryFilter.ALL to recordCounts.total,
                RecordHistoryFilter.THERMOMETER to recordCounts.thermometer,
                RecordHistoryFilter.WORK to recordCounts.work,
                RecordHistoryFilter.REST to recordCounts.rest,
            )
            val columnsPerRow = if (maxWidth.value / fontScale < HeartGuardComponentSize.HistoryCountRowMinWidth.value) {
                COMPACT_COLUMNS_PER_ROW
            } else {
                countColumns.size
            }
            Column(
                modifier = Modifier.padding(vertical = HeartGuardSpacing.Card),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                countColumns.chunked(columnsPerRow).forEach { rowColumns ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        rowColumns.forEachIndexed { columnIndex, (countFilter, recordCount) ->
                            if (columnIndex > 0) {
                                CountDivider()
                            }
                            RecordCountColumn(
                                title = recordHistoryFilterText(countFilter),
                                recordCount = recordCount,
                                hasMoreRecords = hasMoreRecords,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCountColumn(
    title: String,
    recordCount: Int,
    hasMoreRecords: Boolean,
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
            text = if (hasMoreRecords) {
                stringResource(R.string.list_partial_count_format, recordCount)
            } else {
                stringResource(R.string.history_count_format, recordCount)
            },
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

private const val COMPACT_COLUMNS_PER_ROW = 2
