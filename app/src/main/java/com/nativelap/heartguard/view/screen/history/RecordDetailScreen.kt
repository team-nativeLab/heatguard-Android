package com.nativelap.heartguard.view.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.LoadErrorCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.emptyValueText
import com.nativelap.heartguard.view.component.history.RecordDetailInfoCard
import com.nativelap.heartguard.view.component.history.RecordDetailMeasurementCard
import com.nativelap.heartguard.view.component.history.RecordDetailPhotoCard
import com.nativelap.heartguard.view.component.history.RecordDetailSummaryCard
import com.nativelap.heartguard.view.component.history.RecordDetailTextCard
import com.nativelap.heartguard.view.component.history.koreanDateFormatter
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.viewmodel.history.RecordDetailScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordDetailUiState
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Figma 21_기록상세_온도계·22_기록상세_사진 화면이다. 온도계 기록은 측정값·사진·메모를, 사진 기록은 사진과 정보 카드를 보여준다.
 * 위치는 기록을 저장한 당시의 작업 위치·팀명이다. 제출한 기록은 수정할 수 없어 읽기 전용이다. */
@Composable
fun RecordDetailScreen(
    uiState: RecordDetailUiState,
    onEvent: (RecordDetailScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        ResponsivePageContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.history_detail_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(RecordDetailScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                        .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                when (uiState) {
                    RecordDetailUiState.Loading -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HeartGuardSpacing.LargeSection),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    RecordDetailUiState.Failed -> {
                        LoadErrorCard(
                            title = stringResource(R.string.history_detail_load_failure_title),
                            description = stringResource(R.string.history_load_failure_description),
                            onRetryClick = { onEvent(RecordDetailScreenEvent.RetryClicked) },
                        )
                    }

                    is RecordDetailUiState.Loaded -> {
                        RecordDetailContent(
                            recordEntry = uiState.entry,
                            locationText =
                                stringResource(
                                    R.string.history_detail_location_format,
                                    valueOrEmptyText(uiState.entry.workplace),
                                    valueOrEmptyText(uiState.entry.teamName),
                                ),
                        )
                    }
                }
            }
        }
    }
}

private val restClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// "20분 (11:45 ~ 12:05)"처럼 만든다. 서버가 시작·종료 시각을 주지 않으면 저장 시각(measuredAt)을 종료 시각으로 본다.
@Composable
@ReadOnlyComposable
private fun restTimeText(recordEntry: RecordHistoryEntry): String {
    val restMinutes = recordEntry.restMinutes ?: return emptyValueText()
    val restEndedAt =
        recordEntry.restEndedAt
            ?: recordEntry.measuredAt
            ?: return stringResource(R.string.history_detail_rest_minutes_format, restMinutes)
    val restStartedAt = recordEntry.restStartedAt ?: restEndedAt.minusMinutes(restMinutes.toLong())

    return stringResource(
        R.string.history_detail_rest_range_format,
        restMinutes,
        restStartedAt.format(restClockFormatter),
        restEndedAt.format(restClockFormatter),
    )
}

@Composable
private fun RecordDetailContent(
    recordEntry: RecordHistoryEntry,
    locationText: String,
) {
    val measuredAtText =
        recordEntry.measuredAt
            ?.format(koreanDateFormatter(stringResource(R.string.history_detail_datetime_format)))
            ?: emptyValueText()

    if (recordEntry.type == FieldRecordType.THERMOMETER) {
        RecordDetailSummaryCard(
            recordType = recordEntry.type,
            measuredAtText = measuredAtText,
            locationText = locationText,
        )
        RecordDetailMeasurementCard(
            temperature = recordEntry.temperature,
            humidity = recordEntry.humidity,
            apparentTemperature = recordEntry.apparentTemperature,
            heatLevel = recordEntry.heatLevel,
        )
        RecordDetailPhotoCard(
            photoUrls = recordEntry.photoUrls,
            title = stringResource(R.string.history_detail_photo),
        )
    } else {
        RecordDetailPhotoCard(photoUrls = recordEntry.photoUrls)
        RecordDetailInfoCard(
            recordType = recordEntry.type,
            takenAtText = measuredAtText,
            locationText = locationText,
            restTimeText = restTimeText(recordEntry),
        )
    }

    if (recordEntry.memo != null) {
        RecordDetailTextCard(
            title = stringResource(R.string.history_detail_memo),
            bodyText = recordEntry.memo,
        )
    }

    Text(
        text = stringResource(R.string.history_detail_footnote),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.extraColors.secondaryText,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun RecordDetailScreenThermometerPreview() {
    HeartGuardTheme {
        RecordDetailScreen(
            uiState =
                RecordDetailUiState.Loaded(
                    entry =
                        RecordHistoryEntry(
                            recordId = "rec_01",
                            type = FieldRecordType.THERMOMETER,
                            temperature = 36.2,
                            humidity = 65.0,
                            apparentTemperature = 38.7,
                            heatLevel = 1,
                            photoCount = 1,
                            photoUrls = emptyList(),
                            memo = "그늘막 설치 완료",
                            measuredAt = OffsetDateTime.parse("2026-09-27T14:02:00+09:00"),
                        ),
                ),
            onEvent = {},
        )
    }
}
