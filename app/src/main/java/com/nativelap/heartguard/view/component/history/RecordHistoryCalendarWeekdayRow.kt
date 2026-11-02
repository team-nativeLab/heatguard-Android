package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.DayOfWeek

@Composable
internal fun RecordHistoryCalendarWeekdayRow() {
    val weekdayLabels = stringArrayResource(R.array.history_weekday_labels)

    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEachIndexed { index, weekdayLabel ->
            // 요일 배열은 월요일부터 시작한다.
            val isSunday = DayOfWeek.of(index + 1) == DayOfWeek.SUNDAY
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(HeartGuardComponentSize.CalendarWeekdayHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = weekdayLabel,
                    color =
                        if (isSunday) {
                            MaterialTheme.extraColors.calendarSundayText
                        } else {
                            MaterialTheme.extraColors.calendarWeekdayText
                        },
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = HeartGuardFontSize.RecordDateCalendarWeekday,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
        }
    }
}
