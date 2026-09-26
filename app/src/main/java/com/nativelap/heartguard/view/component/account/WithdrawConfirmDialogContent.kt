package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 탈퇴 최종 확인 다이얼로그 본문이다. 경고 배지·질문·설명과 취소/탈퇴하기 버튼을 보여주며,
 * [isSubmitting] 동안에는 두 버튼을 모두 막아 요청이 중복되지 않게 한다. */
@Composable
fun WithdrawConfirmDialogContent(
    title: String,
    message: String,
    cancelTitle: String,
    confirmTitle: String,
    isSubmitting: Boolean,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(max = HeartGuardComponentSize.DialogMaxWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Dialog),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(
                start = HeartGuardSpacing.Section,
                top = HeartGuardSpacing.DialogTop,
                end = HeartGuardSpacing.Section,
                bottom = HeartGuardSpacing.Section,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.DialogIcon)
                    .background(
                        color = MaterialTheme.extraColors.dangerContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    modifier = Modifier.size(HeartGuardIconSize.DialogGlyph),
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Tight))

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = HeartGuardFontSize.DialogTitle,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = HeartGuardFontSize.BodyLineHeight,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Item))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Button(
                    onClick = onCancelClick,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
                    shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.extraColors.strongText,
                    ),
                ) {
                    Text(
                        text = cancelTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                WithdrawDangerButton(
                    title = confirmTitle,
                    onClick = onConfirmClick,
                    isEnabled = !isSubmitting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF666666, widthDp = 360)
@Composable
private fun WithdrawConfirmDialogContentPreview() {
    HeartGuardTheme {
        WithdrawConfirmDialogContent(
            title = "정말 탈퇴하시겠어요?",
            message = "탈퇴하면 계정 정보가 즉시 삭제되며\n다시 되돌릴 수 없어요.",
            cancelTitle = "취소",
            confirmTitle = "탈퇴하기",
            isSubmitting = false,
            onCancelClick = {},
            onConfirmClick = {},
            modifier = Modifier.padding(HeartGuardSpacing.Section),
        )
    }
}
