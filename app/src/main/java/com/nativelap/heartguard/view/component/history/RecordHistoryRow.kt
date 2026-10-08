package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.emptyValueText
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.viewmodel.toDisplayNumber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** 기록 내역 목록의 기록 한 줄이다(Figma 20). 부제는 기록 당시 작업 위치에 유형별 값을 붙인다.
 * 목록 응답에는 사진 URL이 없어 썸네일 없이 표시한다. */
@Composable
fun RecordHistoryRow(
    recordEntry: RecordHistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = HeartGuardComponentSize.TouchTarget)
                    .padding(
                        horizontal = HeartGuardSpacing.Card,
                        vertical = HeartGuardSpacing.Item,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            RecordTypeIconBox(recordType = recordEntry.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recordTypeTitleText(recordEntry.type),
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = recordSubtitleText(recordEntry),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = recordEntry.measuredAt?.format(timeFormatter) ?: emptyValueText(),
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.common_chevron_right),
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// "작업 위치 · 36.2°C · 습도 65%"(온도계), "작업 위치 · 20분 휴식"(휴식), "작업 위치"(작업)처럼 만든다.
@Composable
private fun recordSubtitleText(recordEntry: RecordHistoryEntry): String {
    val separator = stringResource(R.string.history_subtitle_separator)
    val subtitleParts =
        buildList {
            add(valueOrEmptyText(recordEntry.workplace))
            when (recordEntry.type) {
                FieldRecordType.THERMOMETER -> {
                    add(temperatureValueText(recordEntry.temperature.toDisplayNumber()))
                    add(
                        stringResource(
                            R.string.history_humidity_format,
                            valueOrEmptyText(recordEntry.humidity.toDisplayNumber()),
                        ),
                    )
                }

                FieldRecordType.REST -> {
                    add(
                        stringResource(
                            R.string.history_rest_minutes_format,
                            valueOrEmptyText(recordEntry.restMinutes?.toString()),
                        ),
                    )
                }

                FieldRecordType.WORK,
                null,
                -> {
                    Unit
                }
            }
        }
    return subtitleParts.joinToString(separator = separator)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryRowPreview() {
    HeartGuardTheme {
        RecordHistoryRow(
            recordEntry =
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
            onClick = {},
        )
    }
}
