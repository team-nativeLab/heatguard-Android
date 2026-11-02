package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.YearMonth

@Composable
internal fun RecordHistoryCalendarMonthHeader(
    visibleMonth: YearMonth,
    canMoveNext: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val monthTitle =
        stringResource(
            R.string.history_year_month_format,
            visibleMonth.year,
            visibleMonth.monthValue,
        )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RecordHistoryMonthStepButton(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
            label = stringResource(R.string.history_date_picker_previous_month),
            enabled = true,
            onClick = onPreviousClick,
        )

        Text(
            text = monthTitle,
            color = MaterialTheme.extraColors.strongText,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    fontSize = HeartGuardFontSize.RecordDateSheetMonth,
                    fontWeight = FontWeight.Bold,
                ),
        )

        RecordHistoryMonthStepButton(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            label = stringResource(R.string.history_date_picker_next_month),
            enabled = canMoveNext,
            onClick = onNextClick,
        )
    }
}
