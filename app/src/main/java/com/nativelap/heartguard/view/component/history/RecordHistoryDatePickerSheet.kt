package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.view.component.sheet.HeartGuardFigmaBottomSheet
import java.time.LocalDate
import java.time.YearMonth

/** 기록 내역 날짜 선택 시트(Figma 1867:1035)다. 달을 옮기고 날짜를 고른 뒤 "M월 D일 기록 보기"로 확정하면 [onDateConfirmed]를 부른다.
 * 기록이 있는 날은 점으로 표시하고, 오늘 이후 날짜는 고를 수 없다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordHistoryDatePickerSheet(
    selectedDate: LocalDate,
    visibleMonth: YearMonth,
    today: LocalDate,
    recordDayMarkers: Map<LocalDate, Boolean>,
    onMonthMoved: (Int) -> Unit,
    onDateConfirmed: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
) {
    // 확정 전에 고른 날짜다. 회전 후에도 남도록 LocalDateSaver로 저장한다.
    var draftDate by rememberSaveable(selectedDate, stateSaver = LocalDateSaver) { mutableStateOf(selectedDate) }

    HeartGuardFigmaBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Text(
            text = stringResource(R.string.history_date_picker_title),
            color = MaterialTheme.colorScheme.onSurface,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = HeartGuardFontSize.DialogTitle,
                    fontWeight = FontWeight.Bold,
                ),
        )

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

        RecordHistoryCalendarMonthHeader(
            visibleMonth = visibleMonth,
            canMoveNext = visibleMonth.isBefore(YearMonth.from(today)),
            onPreviousClick = { onMonthMoved(-1) },
            onNextClick = { onMonthMoved(1) },
        )

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

        RecordHistoryCalendarWeekdayRow()

        RecordHistoryCalendarGrid(
            visibleMonth = visibleMonth,
            selectedDate = draftDate,
            today = today,
            recordDayMarkers = recordDayMarkers,
            onDateTapped = { date -> draftDate = date },
        )

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

        RecordHistoryDateLegend()

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

        Button(
            onClick = { onDateConfirmed(draftDate) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HeartGuardComponentSize.PrimaryButtonHeight),
            shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.history_date_picker_confirm_format,
                        draftDate.monthValue,
                        draftDate.dayOfMonth,
                    ),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontSize = HeartGuardFontSize.RecordDateCta,
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }
}

// 날짜를 Bundle에 넣을 수 있는 epoch day(Long)로 바꿔 회전 후에도 고른 날짜를 복원한다.
private val LocalDateSaver: Saver<LocalDate, Any> =
    Saver(
        save = { date -> date.toEpochDay() },
        restore = { savedEpochDay -> LocalDate.ofEpochDay(savedEpochDay as Long) },
    )
