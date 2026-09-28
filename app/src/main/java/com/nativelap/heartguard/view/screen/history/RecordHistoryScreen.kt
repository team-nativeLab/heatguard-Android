package com.nativelap.heartguard.view.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.UnavailableFeatureCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent

/** Figma 20_기록내역_목록의 필터 구조를 따르되, 작업자 기록 조회 API가 없어 목록을 비어 있다고 단정하지 않는다. */
@Composable
fun RecordHistoryScreen(
    onEvent: (RecordHistoryScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        ResponsivePageContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.history_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(RecordHistoryScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                    .padding(top = HeartGuardSpacing.Item),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HeartGuardRadius.Card),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = HeartGuardSpacing.Item,
                            vertical = HeartGuardSpacing.Compact,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                    ) {
                        Text(
                            text = stringResource(R.string.history_date_range_unavailable),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.extraColors.disabledText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.common_chevron_down),
                            color = MaterialTheme.extraColors.tertiaryText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact)) {
                    HistoryFilterChip(title = stringResource(R.string.history_filter_all), isSelected = true)
                    HistoryFilterChip(title = stringResource(R.string.history_filter_temperature))
                    HistoryFilterChip(title = stringResource(R.string.history_filter_work_photo))
                    HistoryFilterChip(title = stringResource(R.string.history_filter_rest_photo))
                }

                UnavailableFeatureCard(
                    title = stringResource(R.string.history_api_unavailable_title),
                    description = stringResource(R.string.home_record_history_api_unavailable),
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    title: String,
    isSelected: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = HeartGuardSpacing.Item, vertical = HeartGuardSpacing.Compact),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordHistoryScreenUnavailablePreview() {
    HeartGuardTheme {
        RecordHistoryScreen(onEvent = {})
    }
}
