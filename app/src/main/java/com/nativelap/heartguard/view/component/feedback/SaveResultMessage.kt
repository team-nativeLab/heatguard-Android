package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 결과 배지 안에 표시되는 체크·느낌표 글리프의 크기이다. 원형 배경(HeartGuardIconSize.Result) 안에서 잘리지 않는 비율로 이 파일에서만 한 번 사용된다. */
private val RESULT_BADGE_GLYPH_SIZE = 40.dp

/** 기록 저장 결과 화면의 상태별 아이콘, 제목, 설명을 표시한다.
 *  Figma 09_저장성공/10_저장실패 스펙: 흰 배경 + cardBorder 테두리 + LargeCard 라운드 카드 위에
 *  성공/실패 색상의 원형 배지(배경 원 + 글리프 아이콘)를 겹쳐 그린다. */
@Composable
fun SaveResultMessage(
    title: String,
    description: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Box(
                modifier = Modifier.size(HeartGuardIconSize.Result),
                contentAlignment = Alignment.Center,
            ) {
                // 결과 상태를 드러내는 원형 배경이다. 성공은 연두색, 실패는 연분홍 컨테이너 색상을 사용한다.
                Box(
                    modifier = Modifier
                        .size(HeartGuardIconSize.Result)
                        .background(
                            color = if (isSuccess) {
                                MaterialTheme.extraColors.successContainer
                            } else {
                                MaterialTheme.extraColors.alertContainer
                            },
                            shape = CircleShape,
                        ),
                )
                // 아래 제목 텍스트가 이미 같은 의미를 전달하므로 아이콘은 장식용으로 처리한다.
                Icon(
                    imageVector = if (isSuccess) Icons.Filled.Check else Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = if (isSuccess) {
                        MaterialTheme.extraColors.success
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(RESULT_BADGE_GLYPH_SIZE),
                )
            }
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
            title = "저장되었습니다",
            description = "기록이 정상적으로 저장되었습니다",
            isSuccess = true,
        )
    }
}
