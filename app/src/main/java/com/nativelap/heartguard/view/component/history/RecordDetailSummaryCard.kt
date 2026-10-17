package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 상세 상단 카드다(Figma 21). 유형 칩·측정 일시·"작업 위치 · 팀명"을 보여준다. */
@Composable
fun RecordDetailSummaryCard(
    recordType: FieldRecordType?,
    measuredAtText: String,
    locationText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            RecordTypeChip(recordType = recordType)
            Text(
                text = measuredAtText,
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = locationText,
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordDetailSummaryCardPreview() {
    HeartGuardTheme {
        RecordDetailSummaryCard(
            recordType = FieldRecordType.THERMOMETER,
            measuredAtText = "2026.09.27 (일) 14:02",
            locationText = "3층 외벽 · 홍길동 팀",
        )
    }
}
