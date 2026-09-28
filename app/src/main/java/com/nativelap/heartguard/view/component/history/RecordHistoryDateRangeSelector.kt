package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import java.time.LocalDate

/** 기록 내역의 조회 기간 카드다. 누르면 기간 선택 다이얼로그를 연다. */
@Composable
fun RecordHistoryDateRangeSelector(
    startDate: LocalDate,
    endDate: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = koreanDateFormatter(stringResource(R.string.history_date_format))
    val rangeDescription = stringResource(R.string.history_date_range_description)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = rangeDescription },
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = HeartGuardComponentSize.TouchTarget)
                .padding(
                    horizontal = HeartGuardSpacing.Card,
                    vertical = HeartGuardSpacing.Item,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(HeartGuardIconSize.Navigation),
                tint = MaterialTheme.extraColors.secondaryText,
            )
            Text(
                text = startDate.format(dateFormatter),
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = stringResource(R.string.history_date_range_separator),
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = endDate.format(dateFormatter),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.extraColors.tertiaryText,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryDateRangeSelectorPreview() {
    HeartGuardTheme {
        RecordHistoryDateRangeSelector(
            startDate = LocalDate.of(2026, 9, 21),
            endDate = LocalDate.of(2026, 9, 27),
            onClick = {},
        )
    }
}
