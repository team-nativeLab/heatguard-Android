package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 기록 조회 기간을 고르는 다이얼로그다. 오늘 이후 날짜는 고를 수 없고, 시작·종료일이 모두 있어야 확인할 수 있다.
 * Material3 DatePicker는 날짜를 UTC 자정 millis로 다루므로 LocalDate와 UTC 기준으로 변환한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordHistoryDateRangePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    today: LocalDate,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.toUtcMillis()
    val selectableDates =
        remember(todayMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            }
        }
    val dateRangePickerState =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialStartDate.toUtcMillis(),
            initialSelectedEndDateMillis = initialEndDate.toUtcMillis(),
            selectableDates = selectableDates,
        )
    val selectedStartMillis = dateRangePickerState.selectedStartDateMillis
    val selectedEndMillis = dateRangePickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedStartMillis != null && selectedEndMillis != null) {
                        onConfirm(
                            selectedStartMillis.toUtcLocalDate(),
                            selectedEndMillis.toUtcLocalDate(),
                        )
                    }
                },
                enabled = selectedStartMillis != null && selectedEndMillis != null,
            ) {
                Text(text = stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Column(
                    modifier =
                        Modifier.padding(
                            start = HeartGuardSpacing.LargeSection,
                            end = HeartGuardSpacing.LargeSection,
                            top = HeartGuardSpacing.Card,
                        ),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Text(text = stringResource(R.string.history_date_picker_title))
                    Text(
                        text = stringResource(R.string.history_date_picker_hint),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            headline = null,
            showModeToggle = false,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
