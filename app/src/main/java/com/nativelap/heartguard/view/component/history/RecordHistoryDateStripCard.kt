package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.LocalDate

/** 기록 내역 상단의 날짜 카드다. 첫 줄은 "2026년 9월"(누르면 날짜 선택 시트)과 "오늘" 칩이고, 아래는 선택한 날짜가 속한 주의 7칸 스트립이다. */
@Composable
fun RecordHistoryDateStripCard(
    selectedDate: LocalDate,
    today: LocalDate,
    weekDates: List<LocalDate>,
    recordDayMarkers: Map<LocalDate, Boolean>,
    onMonthClick: () -> Unit,
    onTodayClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthTitle =
        stringResource(
            R.string.history_year_month_format,
            selectedDate.year,
            selectedDate.monthValue,
        )
    val pickerDescription = stringResource(R.string.history_date_picker_title)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.Card,
                    vertical = HeartGuardSpacing.DateStripVertical,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = HeartGuardComponentSize.TouchTarget)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = pickerDescription,
                                onClick = onMonthClick,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = monthTitle,
                        color = MaterialTheme.extraColors.strongText,
                        style =
                            MaterialTheme.typography.bodyLarge.copy(
                                fontSize = HeartGuardFontSize.RecordDateStripTitle,
                                fontWeight = FontWeight.Bold,
                            ),
                    )

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.extraColors.captionText,
                    )
                }

                // 칩 배경은 글자 줄 높이에 위아래 여백을 더한 크기이고, 누를 수 있는 영역은 48dp 높이를 유지한다.
                Box(
                    modifier =
                        Modifier
                            .heightIn(min = HeartGuardComponentSize.TouchTarget)
                            .clickable(
                                role = Role.Button,
                                onClick = onTodayClick,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.history_today),
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = HeartGuardSpacing.TodayChipHorizontal,
                                    vertical = HeartGuardSpacing.TodayChipVertical,
                                ).background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(HeartGuardRadius.Pill),
                                ),
                        color = MaterialTheme.colorScheme.primary,
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontSize = HeartGuardFontSize.RecordDateChip,
                                fontWeight = FontWeight.Medium,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

            RecordHistoryWeekStrip(
                weekDates = weekDates,
                selectedDate = selectedDate,
                today = today,
                recordDayMarkers = recordDayMarkers,
                onDateClick = onDateClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryDateStripCardPreview() {
    HeartGuardTheme {
        RecordHistoryDateStripCard(
            selectedDate = LocalDate.of(2026, 9, 27),
            today = LocalDate.of(2026, 9, 27),
            weekDates = (21..27).map { day -> LocalDate.of(2026, 9, day) },
            recordDayMarkers = mapOf(LocalDate.of(2026, 9, 21) to true, LocalDate.of(2026, 9, 27) to true),
            onMonthClick = {},
            onTodayClick = {},
            onDateClick = {},
        )
    }
}
