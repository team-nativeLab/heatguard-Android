package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 유형을 나타내는 아이콘 상자다. 휴식 사진은 초록 배경, 나머지는 파란 배경으로 구분한다(Figma 20·21). 장식용이라 설명을 두지 않는다. */
@Composable
fun RecordTypeIconBox(
    recordType: FieldRecordType?,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (recordType == FieldRecordType.REST) {
        MaterialTheme.extraColors.successContainer
    } else {
        MaterialTheme.extraColors.infoContainer
    }

    Box(
        modifier = modifier
            .size(HeartGuardIconSize.RecordHistoryType)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(HeartGuardRadius.HomeAction),
            )
            .padding(HeartGuardSpacing.Compact),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(recordTypeIconRes(recordType)),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun RecordTypeIconBoxPreview() {
    HeartGuardTheme {
        RecordTypeIconBox(recordType = FieldRecordType.REST)
    }
}
