package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 탈퇴 전 알아야 할 유의사항을 경고 아이콘 제목과 점 목록으로 보여주는 흰 카드다. */
@Composable
fun WithdrawNoticeCard(
    title: String,
    noticeLines: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.NoticeCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Box(
                    modifier = Modifier
                        .size(HeartGuardIconSize.WarningBadge)
                        .background(
                            color = MaterialTheme.extraColors.dangerContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        modifier = Modifier.size(HeartGuardIconSize.Small),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = title,
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = HeartGuardFontSize.CardTitle,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            noticeLines.forEach { noticeLine ->
                WithdrawNoticeLine(text = noticeLine)
            }
        }
    }
}

/** 유의사항 한 줄을 앞쪽 점과 함께 보여준다. 줄이 바뀌어도 점은 첫 줄에 맞춰 위쪽에 둔다. */
@Composable
private fun WithdrawNoticeLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        Box(
            modifier = Modifier
                .padding(top = HeartGuardSpacing.Compact)
                .size(HeartGuardIconSize.Bullet)
                .background(
                    color = MaterialTheme.extraColors.tertiaryText,
                    shape = CircleShape,
                ),
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = HeartGuardFontSize.BodyLineHeight,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6F9FC, widthDp = 354)
@Composable
private fun WithdrawNoticeCardPreview() {
    HeartGuardTheme {
        WithdrawNoticeCard(
            title = "유의사항",
            noticeLines = listOf(
                "계정 정보(이름, 이메일, 회사명)는 즉시 삭제되며 복구할 수 없어요",
                "온도계 기록·현장 사진 등 작업 기록은 관계 법령에 따라 회사에 일정 기간 보관될 수 있어요",
                "탈퇴 후 같은 이메일로 다시 가입해도 이전 기록은 연결되지 않아요",
            ),
        )
    }
}
