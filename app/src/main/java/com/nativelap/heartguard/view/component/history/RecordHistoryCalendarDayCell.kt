package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
internal fun RecordHistoryCalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isEnabled: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateLabel =
        stringResource(
            if (hasRecord) {
                R.string.history_date_cell_with_record_format
            } else {
                R.string.history_date_cell_format
            },
            date.monthValue,
            date.dayOfMonth,
        )
    val numberColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            !isEnabled -> MaterialTheme.extraColors.calendarDisabledText
            date.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.extraColors.calendarSundayText
            else -> MaterialTheme.extraColors.strongText
        }
    val selectionBackground =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        }

    Column(
        modifier =
            modifier
                .height(HeartGuardComponentSize.CalendarDayHeight)
                .clickable(
                    enabled = isEnabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = dateLabel
                    selected = isSelected
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardComponentSize.CalendarDaySelection)
                    .clip(CircleShape)
                    .background(selectionBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = numberColor,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = HeartGuardFontSize.RecordDateCalendarDay,
                        fontWeight =
                            if (isSelected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                    ),
            )

            RecordHistoryRecordDot(
                hasRecord = hasRecord,
                isSelected = isSelected,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
