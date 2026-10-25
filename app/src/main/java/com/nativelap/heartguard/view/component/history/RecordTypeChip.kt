package com.nativelap.heartguard.view.component.history

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

/** 기록 유형 이름 칩이다. 휴식 사진은 초록, 나머지는 파랑으로 구분한다(Figma 21·22). */
@Composable
fun RecordTypeChip(
    recordType: FieldRecordType?,
    modifier: Modifier = Modifier,
) {
    val isRestRecord = recordType == FieldRecordType.REST
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HeartGuardRadius.Pill),
        color =
            if (isRestRecord) {
                MaterialTheme.extraColors.successContainer
            } else {
                MaterialTheme.extraColors.infoContainer
            },
        contentColor =
            if (isRestRecord) {
                MaterialTheme.extraColors.success
            } else {
                MaterialTheme.colorScheme.primary
            },
    ) {
        Text(
            text = recordTypeTitleText(recordType),
            modifier =
                Modifier.padding(
                    horizontal = HeartGuardSpacing.BadgeHorizontal,
                    vertical = HeartGuardSpacing.Tight,
                ),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Preview
@Composable
private fun RecordTypeChipPreview() {
    HeartGuardTheme {
        RecordTypeChip(recordType = FieldRecordType.REST)
    }
}
