package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 사진 기록 정보 카드다(Figma 22). 유형·촬영 시간·위치를 구분선으로 나눠 보여주고, 휴식 사진이면 휴식 시간 행을 더한다. */
@Composable
fun RecordDetailInfoCard(
    recordType: FieldRecordType?,
    takenAtText: String,
    locationText: String,
    restTimeText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = HeartGuardSpacing.Section),
        ) {
            InfoRow(label = stringResource(R.string.history_detail_type)) {
                RecordTypeChip(recordType = recordType)
            }
            InfoRowDivider()
            InfoRow(label = stringResource(R.string.history_detail_taken_at)) {
                InfoValueText(valueText = takenAtText)
            }
            InfoRowDivider()
            InfoRow(label = stringResource(R.string.history_detail_location)) {
                InfoValueText(valueText = locationText)
            }
            if (recordType == FieldRecordType.REST) {
                InfoRowDivider()
                InfoRow(label = stringResource(R.string.history_detail_rest_time)) {
                    InfoValueText(valueText = restTimeText)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    valueContent: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.DetailInfoRowHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium,
        )
        valueContent()
    }
}

@Composable
private fun InfoRowDivider() {
    HorizontalDivider(
        thickness = HeartGuardBorderWidth.Divider,
        color = MaterialTheme.extraColors.cardBorder,
    )
}

@Composable
private fun InfoValueText(valueText: String) {
    Text(
        text = valueText,
        color = MaterialTheme.extraColors.strongText,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.End,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordDetailInfoCardPreview() {
    HeartGuardTheme {
        RecordDetailInfoCard(
            recordType = FieldRecordType.REST,
            takenAtText = "2026.09.27 (일) 12:05",
            locationText = "옥상 그늘막 · 홍길동 팀",
            restTimeText = "20분 (11:45 ~ 12:05)",
        )
    }
}
