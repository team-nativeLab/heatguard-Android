package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter

/** 조회 기간·필터에 맞는 기록이 없을 때의 안내다(Figma 23). 선택한 필터 이름을 문구에 넣는다. */
@Composable
fun RecordHistoryEmptyState(
    selectedFilter: RecordHistoryFilter,
    modifier: Modifier = Modifier,
) {
    val emptyTitle =
        if (selectedFilter == RecordHistoryFilter.ALL) {
            stringResource(R.string.history_empty_title_all)
        } else {
            stringResource(
                R.string.history_empty_title_format,
                recordHistoryFilterText(selectedFilter),
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = HeartGuardSpacing.LargeSection),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardIconSize.EmptyStateIllustration)
                    .background(
                        color = MaterialTheme.extraColors.infoContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = emptyTitle,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_empty_description),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryEmptyStatePreview() {
    HeartGuardTheme {
        RecordHistoryEmptyState(selectedFilter = RecordHistoryFilter.REST)
    }
}
