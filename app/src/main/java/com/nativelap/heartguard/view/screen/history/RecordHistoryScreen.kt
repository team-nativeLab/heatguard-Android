package com.nativelap.heartguard.view.screen.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.LoadErrorCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.history.RecordHistoryCountSummaryCard
import com.nativelap.heartguard.view.component.history.RecordHistoryDateRangeSelector
import com.nativelap.heartguard.view.component.history.RecordHistoryDayCard
import com.nativelap.heartguard.view.component.history.RecordHistoryDayHeader
import com.nativelap.heartguard.view.component.history.RecordHistoryEmptyState
import com.nativelap.heartguard.view.component.history.RecordHistoryFilterRow
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.history.RecordHistoryLoadState
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordHistoryUiState
import java.time.LocalDate
import java.time.OffsetDateTime

/** Figma 20_기록내역_목록·23_기록내역_빈상태 화면이다. 기간·유형 필터·유형별 건수·날짜별 기록 목록을 보여준다.
 * 빈 결과일 때만 하단에 "기록하기" 버튼을 둔다. */
@Composable
fun RecordHistoryScreen(
    uiState: RecordHistoryUiState,
    onEvent: (RecordHistoryScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    temporaryDraftType: RecordType? = null,
    temporaryDraftSavedAt: OffsetDateTime? = null,
) {
    val loadState = uiState.loadState
    val dayGroups = uiState.dayGroups
    val temporaryDraftDate = temporaryDraftSavedAt?.toLocalDate() ?: uiState.today
    val visibleTemporaryDraftType = temporaryDraftType?.takeIf { draftType ->
        draftType.matches(uiState.selectedFilter) &&
            !temporaryDraftDate.isBefore(uiState.startDate) &&
            !temporaryDraftDate.isAfter(uiState.endDate)
    }
    val isEmptyResult = loadState is RecordHistoryLoadState.Loaded &&
        dayGroups.isEmpty() &&
        visibleTemporaryDraftType == null

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            if (isEmptyResult) {
                BottomActionBar {
                    RecordSaveButton(
                        title = stringResource(R.string.history_create_record),
                        onClick = { onEvent(RecordHistoryScreenEvent.CreateRecordClicked) },
                    )
                }
            }
        },
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = HeartGuardSpacing.AccountContentHorizontal,
                    end = HeartGuardSpacing.AccountContentHorizontal,
                    top = HeartGuardSpacing.Item,
                    bottom = HeartGuardSpacing.Section,
                ),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                item(key = "dateRange") {
                    RecordHistoryDateRangeSelector(
                        startDate = uiState.startDate,
                        endDate = uiState.endDate,
                        onClick = { onEvent(RecordHistoryScreenEvent.DateRangeClicked) },
                    )
                }
                item(key = "filters") {
                    RecordHistoryFilterRow(
                        selectedFilter = uiState.selectedFilter,
                        onFilterClick = { filter ->
                            onEvent(RecordHistoryScreenEvent.FilterSelected(filter))
                        },
                    )
                }

                when (loadState) {
                    RecordHistoryLoadState.Loading -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HeartGuardSpacing.LargeSection),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    RecordHistoryLoadState.Failed -> {
                        item(key = "error") {
                            LoadErrorCard(
                                title = stringResource(R.string.history_load_failure_title),
                                description = stringResource(R.string.history_load_failure_description),
                                onRetryClick = { onEvent(RecordHistoryScreenEvent.RetryClicked) },
                            )
                        }
                    }

                    is RecordHistoryLoadState.Loaded -> {
                        if (isEmptyResult) {
                            item(key = "empty") {
                                RecordHistoryEmptyState(selectedFilter = uiState.selectedFilter)
                            }
                        } else {
                            item(key = "counts") {
                                RecordHistoryCountSummaryCard(recordCounts = uiState.recordCounts)
                            }
                            val todayGroup = dayGroups.firstOrNull { dayGroup ->
                                dayGroup.date == uiState.today
                            }
                            if (visibleTemporaryDraftType != null) {
                                item(key = "header-${uiState.today}") {
                                    RecordHistoryDayHeader(
                                        date = uiState.today,
                                        today = uiState.today,
                                    )
                                }
                                item(key = "day-${uiState.today}") {
                                    RecordHistoryDayCard(
                                        recordEntries = todayGroup?.entries.orEmpty(),
                                        temporaryDraftType = visibleTemporaryDraftType,
                                        temporaryDraftSavedAt = temporaryDraftSavedAt,
                                        onTemporaryDraftClick = {
                                            onEvent(RecordHistoryScreenEvent.TemporaryDraftClicked)
                                        },
                                        onRecordClick = { recordId ->
                                            onEvent(RecordHistoryScreenEvent.RecordClicked(recordId))
                                        },
                                    )
                                }
                            }
                            dayGroups
                                .filterNot { dayGroup ->
                                    visibleTemporaryDraftType != null && dayGroup.date == uiState.today
                                }
                                .forEach { dayGroup ->
                                item(key = "header-${dayGroup.date}") {
                                    RecordHistoryDayHeader(
                                        date = dayGroup.date,
                                        today = uiState.today,
                                    )
                                }
                                item(key = "day-${dayGroup.date}") {
                                    RecordHistoryDayCard(
                                        recordEntries = dayGroup.entries,
                                        onRecordClick = { recordId ->
                                            onEvent(RecordHistoryScreenEvent.RecordClicked(recordId))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun RecordType.matches(filter: RecordHistoryFilter): Boolean = when (filter) {
    RecordHistoryFilter.ALL -> true
    RecordHistoryFilter.THERMOMETER -> this == RecordType.TEMPERATURE
    RecordHistoryFilter.WORK -> this == RecordType.WORK
    RecordHistoryFilter.REST -> this == RecordType.REST
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordHistoryScreenLoadedPreview() {
    HeartGuardTheme {
        RecordHistoryScreen(
            uiState = RecordHistoryUiState(
                today = LocalDate.of(2026, 9, 27),
                startDate = LocalDate.of(2026, 9, 21),
                endDate = LocalDate.of(2026, 9, 27),
                loadState = RecordHistoryLoadState.Loaded(
                    entries = listOf(
                        RecordHistoryEntry(
                            recordId = "rec_01",
                            type = FieldRecordType.THERMOMETER,
                            temperature = 36.2,
                            humidity = 65.0,
                            apparentTemperature = 38.7,
                            heatLevel = 1,
                            photoCount = 1,
                            photoUrls = emptyList(),
                            memo = null,
                            measuredAt = OffsetDateTime.parse("2026-09-27T14:02:00+09:00"),
                        ),
                        RecordHistoryEntry(
                            recordId = "rec_02",
                            type = FieldRecordType.REST,
                            temperature = null,
                            humidity = null,
                            apparentTemperature = null,
                            heatLevel = null,
                            photoCount = 2,
                            photoUrls = emptyList(),
                            memo = null,
                            measuredAt = OffsetDateTime.parse("2026-09-26T12:10:00+09:00"),
                        ),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordHistoryScreenEmptyPreview() {
    HeartGuardTheme {
        RecordHistoryScreen(
            uiState = RecordHistoryUiState(
                today = LocalDate.of(2026, 9, 27),
                startDate = LocalDate.of(2026, 9, 21),
                endDate = LocalDate.of(2026, 9, 27),
                loadState = RecordHistoryLoadState.Loaded(entries = emptyList()),
            ),
            onEvent = {},
        )
    }
}
