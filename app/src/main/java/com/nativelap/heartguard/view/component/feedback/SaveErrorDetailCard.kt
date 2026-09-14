package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.BorderStroke
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
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 저장 실패 화면에서 사용자가 재시도할 수 있도록 원인 안내를 강조한다.
 *  Figma 10_저장실패 스펙: 흰 배경 + cardBorder 테두리를 사용한다(강조 배경색 아님). */
@Composable
fun SaveErrorDetailCard(
    title: String,
    details: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            details.forEach { detail ->
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun SaveErrorDetailCardPreview() {
    HeartGuardTheme {
        SaveErrorDetailCard(
            title = "저장하지 못했습니다",
            details = listOf("네트워크 연결을 확인해 주세요", "잠시 후 다시 시도해 주세요"),
        )
    }
}
