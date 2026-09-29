package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.RecordType
import java.time.OffsetDateTime

/** 같은 날 기록들을 구분선으로 나눠 한 카드에 담는다(Figma 20). */
@Composable
fun RecordHistoryDayCard(
    recordEntries: List<RecordHistoryEntry>,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    temporaryDraftType: RecordType? = null,
    temporaryDraftSavedAt: OffsetDateTime? = null,
    onTemporaryDraftClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            if (temporaryDraftType != null) {
                RecordHistoryTemporaryDraftRow(
                    recordType = temporaryDraftType,
                    savedAt = temporaryDraftSavedAt,
                    onClick = onTemporaryDraftClick,
                )
            }
            recordEntries.forEachIndexed { entryIndex, recordEntry ->
                if (entryIndex > 0 || temporaryDraftType != null) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = HeartGuardBorderWidth.Divider,
                        color = MaterialTheme.extraColors.cardBorder,
                    )
                }
                RecordHistoryRow(
                    recordEntry = recordEntry,
                    onClick = { onRecordClick(recordEntry.recordId) },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordHistoryDayCardPreview() {
    HeartGuardTheme {
        RecordHistoryDayCard(
            recordEntries = listOf(
                RecordHistoryEntry(
                    recordId = "rec_01",
                    type = FieldRecordType.WORK,
                    temperature = null,
                    humidity = null,
                    apparentTemperature = null,
                    heatLevel = null,
                    photoCount = 2,
                    photoUrls = emptyList(),
                    memo = null,
                    measuredAt = OffsetDateTime.parse("2026-09-27T10:30:00+09:00"),
                ),
            ),
            onRecordClick = {},
        )
    }
}
