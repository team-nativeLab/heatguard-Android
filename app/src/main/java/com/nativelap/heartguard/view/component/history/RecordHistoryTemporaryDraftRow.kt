package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.RecordType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** 현재 세션에서 임시 보관한 초안을 기록 목록에 표시한다. */
@Composable
fun RecordHistoryTemporaryDraftRow(
    recordType: RecordType,
    savedAt: OffsetDateTime?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldRecordType = recordType.toFieldRecordType()

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
            RecordTypeIconBox(recordType = fieldRecordType)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                ) {
                    Text(
                        text = recordTypeTitleText(fieldRecordType),
                        color = MaterialTheme.extraColors.strongText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        shape = RoundedCornerShape(HeartGuardRadius.Pill),
                        color = MaterialTheme.extraColors.warningContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.history_temporary_draft_badge),
                            modifier =
                                Modifier.padding(
                                    horizontal = HeartGuardSpacing.Compact,
                                    vertical = HeartGuardSpacing.Tight,
                                ),
                            color = MaterialTheme.extraColors.onWarningContainer,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.history_temporary_draft_not_saved),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = savedAt?.format(savedAtFormatter).orEmpty(),
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

private fun RecordType.toFieldRecordType(): FieldRecordType =
    when (this) {
        RecordType.TEMPERATURE -> FieldRecordType.THERMOMETER
        RecordType.WORK -> FieldRecordType.WORK
        RecordType.REST -> FieldRecordType.REST
    }

private val savedAtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun RecordHistoryTemporaryDraftRowPreview() {
    HeartGuardTheme {
        RecordHistoryTemporaryDraftRow(
            recordType = RecordType.TEMPERATURE,
            savedAt = OffsetDateTime.parse("2026-09-27T16:00:00+09:00"),
            onClick = {},
        )
    }
}
