package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import java.time.LocalDate

/** 선택한 날짜가 속한 주의 7칸 스트립이다. [weekDates]는 월요일부터 일요일까지 순서대로 받는다.
 * 오늘 이후 날짜는 고를 수 없고, 기록이 있는 날은 칸 아래에 점을 표시한다. */
@Composable
fun RecordHistoryWeekStrip(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDayMarkers: Map<LocalDate, Boolean>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekdayLabels = stringArrayResource(R.array.history_weekday_labels)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
    ) {
        weekDates.forEach { date ->
            RecordHistoryWeekDayCell(
                date = date,
                weekdayLabel = weekdayLabels[date.dayOfWeek.value - 1],
                isSelected = date == selectedDate,
                isEnabled = !date.isAfter(today),
                hasRecord = recordDayMarkers[date] == true,
                onClick = { onDateClick(date) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryWeekStripPreview() {
    HeartGuardTheme {
        RecordHistoryWeekStrip(
            weekDates = (21..27).map { day -> LocalDate.of(2026, 9, day) },
            selectedDate = LocalDate.of(2026, 9, 24),
            today = LocalDate.of(2026, 9, 27),
            recordDayMarkers = mapOf(LocalDate.of(2026, 9, 21) to true, LocalDate.of(2026, 9, 24) to true),
            onDateClick = {},
        )
    }
}
