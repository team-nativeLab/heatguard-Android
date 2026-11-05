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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.LocalDate

// '오늘' 칩 글자의 줄 높이다. Figma 1864:1391에서 칩 높이 22dp가 여백 4dp와 이 줄 높이로 나온다.
private val TodayChipLineHeight = 14.sp

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

                // 배경 크기와 터치 영역을 분리해 디자인 크기와 접근성 타깃을 모두 유지한다.
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
                    Box(
                        modifier =
                            Modifier
                                .sizeIn(
                                    minWidth = HeartGuardComponentSize.TodayChipMinWidth,
                                    minHeight = HeartGuardComponentSize.TodayChipMinHeight,
                                ).background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(HeartGuardRadius.Pill),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.history_today),
                            color = MaterialTheme.colorScheme.primary,
                            style =
                                MaterialTheme.typography.labelLarge.copy(
                                    fontSize = HeartGuardFontSize.RecordDateChip,
                                    lineHeight = TodayChipLineHeight,
                                    fontWeight = FontWeight.Medium,
                                ),
                        )
                    }
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
