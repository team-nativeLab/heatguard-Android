package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import java.time.LocalDate
import java.time.YearMonth

/** 날짜 선택 시트의 달력 격자다. 월요일 시작 7칸씩 주 단위로 그리며, 오늘 이후 날짜는 고를 수 없다. */
@Composable
internal fun RecordHistoryCalendarGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    recordDayMarkers: Map<LocalDate, Boolean>,
    onDateTapped: (LocalDate) -> Unit,
) {
    val dayCells = calendarDayCells(visibleMonth)

    Column(modifier = Modifier.fillMaxWidth()) {
        dayCells.chunked(DAYS_PER_WEEK).forEach { weekCells ->
            Row(modifier = Modifier.fillMaxWidth()) {
                weekCells.forEach { date ->
                    if (date == null) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(HeartGuardComponentSize.CalendarDayHeight),
                        )
                    } else {
                        RecordHistoryCalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isEnabled = !date.isAfter(today),
                            hasRecord = recordDayMarkers[date] == true,
                            onClick = { onDateTapped(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 달력 칸을 월요일 시작 기준으로 만든다. 달의 주 수와 관계없이 6주(42칸)로 채워 시트 높이가 달마다 바뀌지 않게 한다.
 * 첫 날 앞과 뒤쪽 빈 칸은 null로 두고, 호출부가 7칸씩 끊어 쓴다.
 */
private fun calendarDayCells(visibleMonth: YearMonth): List<LocalDate?> {
    val leadingBlankCount = visibleMonth.atDay(1).dayOfWeek.value - 1
    val leadingCells: List<LocalDate?> = List(leadingBlankCount) { null }
    val monthCells: List<LocalDate?> = (1..visibleMonth.lengthOfMonth()).map { day -> visibleMonth.atDay(day) }
    val cells = leadingCells + monthCells
    val trailingBlankCount = GRID_CELL_COUNT - cells.size
    val trailingCells: List<LocalDate?> = List(trailingBlankCount) { null }
    return cells + trailingCells
}

private const val DAYS_PER_WEEK = 7
private const val GRID_WEEK_COUNT = 6
private const val GRID_CELL_COUNT = DAYS_PER_WEEK * GRID_WEEK_COUNT
