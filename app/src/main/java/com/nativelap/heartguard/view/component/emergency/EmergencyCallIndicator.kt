package com.nativelap.heartguard.view.component.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 긴급 호출 전·진행 상태를 Figma 원형 에셋과 설명 문구로 보여준다. */
@Composable
fun EmergencyCallIndicator(
    title: String,
    description: String,
    isCalling: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Box(
            modifier = Modifier
                .size(HeartGuardIconSize.EmergencyCall)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // emergency_outer/emergency_inner 에셋은 투명 배경이 아니라 캔버스 전체를 불투명
            // 검정으로 채운 채 내보내져 있어(clip으로도 모서리를 다 가릴 수 없다), 대신 상태
            // 색상의 원을 직접 그린다. isCalling 상태에 따라 title 텍스트와 같은 색상 규칙
            // (빨강/주황)을 적용해 원 색상도 함께 바뀌도록 한다.
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.EmergencyCall)
                    .background(
                        color = if (isCalling) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.extraColors.warningContainer
                        },
                        shape = CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.EmergencyCallInner)
                    .background(
                        color = if (isCalling) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        shape = CircleShape,
                    ),
            )
            Text(
                text = "⚠️",
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Text(
            text = title,
            color = if (isCalling) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyCallIndicatorPreview() {
    HeartGuardTheme {
        EmergencyCallIndicator(
            title = "긴급 호출하기",
            description = "버튼을 누르면 즉시 관리자에게 전화가 연결됩니다",
            isCalling = true,
            onClick = {},
        )
    }
}
