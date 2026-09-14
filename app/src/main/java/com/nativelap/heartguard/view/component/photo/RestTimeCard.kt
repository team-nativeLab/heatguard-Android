package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 휴식 사진 기록에서 휴식 시간을 선택하는 라벨과 값 박스다. */
@Composable
fun RestTimeCard(
    title: String,
    selectedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )

        Surface(
            modifier = Modifier
                .padding(top = HeartGuardSpacing.Compact)
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
            shape = RoundedCornerShape(HeartGuardRadius.Card),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(HeartGuardSpacing.Hairline, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(HeartGuardSpacing.Item),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectedTime,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun RestTimeCardPreview() {
    HeartGuardTheme {
        RestTimeCard(title = "휴식 시간", selectedTime = "13 : 00 ~ 13 : 30 (중간 휴식)", onClick = {})
    }
}
