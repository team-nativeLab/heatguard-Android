package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter

/** 전체·온도계·작업 사진·휴식 사진 필터 칩 줄이다. 좁은 화면이나 큰 글꼴에서도 잘리지 않도록 가로로 스크롤된다. */
@Composable
fun RecordHistoryFilterRow(
    selectedFilter: RecordHistoryFilter,
    onFilterClick: (RecordHistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        RecordHistoryFilter.entries.forEach { filter ->
            RecordHistoryFilterChip(
                title = recordHistoryFilterText(filter),
                isSelected = filter == selectedFilter,
                onClick = { onFilterClick(filter) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryFilterRowPreview() {
    HeartGuardTheme {
        RecordHistoryFilterRow(
            selectedFilter = RecordHistoryFilter.ALL,
            onFilterClick = {},
        )
    }
}
