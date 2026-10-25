package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.LocalDate

/** 날짜별 기록 묶음 제목이다. 오늘·어제는 접두어를 붙인다(예: "오늘 · 9월 27일 (일)"). [date]가 null이면 측정 시각 없음이다. */
@Composable
fun RecordHistoryDayHeader(
    date: LocalDate?,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val dayText =
        if (date == null) {
            stringResource(R.string.history_day_unknown)
        } else {
            val formattedDate = date.format(koreanDateFormatter(stringResource(R.string.history_day_format)))
            when (date) {
                today -> stringResource(R.string.history_day_today_format, formattedDate)
                today.minusDays(1) -> stringResource(R.string.history_day_yesterday_format, formattedDate)
                else -> formattedDate
            }
        }

    Text(
        text = dayText,
        modifier =
            modifier
                .padding(
                    start = HeartGuardSpacing.Tight,
                    top = HeartGuardSpacing.Compact,
                ).semantics { heading() },
        color = MaterialTheme.extraColors.secondaryText,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordHistoryDayHeaderPreview() {
    HeartGuardTheme {
        RecordHistoryDayHeader(
            date = LocalDate.of(2026, 9, 27),
            today = LocalDate.of(2026, 9, 27),
        )
    }
}
