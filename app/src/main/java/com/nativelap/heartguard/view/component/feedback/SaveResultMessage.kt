package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 저장 결과 화면의 상태별 아이콘, 제목, 설명을 표시한다. */
@Composable
fun SaveResultMessage(
    resultIconPainter: Painter,
    resultIconContentDescription: String,
    title: String,
    description: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Image(
                painter = resultIconPainter,
                contentDescription = resultIconContentDescription,
                modifier = Modifier.size(HeartGuardIconSize.Result),
            )
            Text(
                text = title,
                color = if (isSuccess) {
                    MaterialTheme.extraColors.success
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun SaveResultMessagePreview() {
    HeartGuardTheme {
        SaveResultMessage(
            resultIconPainter = androidx.compose.ui.res.painterResource(
                com.nativelap.heartguard.R.drawable.record_type_selected,
            ),
            resultIconContentDescription = "저장 완료",
            title = "저장되었습니다",
            description = "기록이 정상적으로 저장되었습니다",
            isSuccess = true,
        )
    }
}
