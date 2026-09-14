package com.nativelap.heartguard.view.component.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 긴급호출 전·진행 상태를 큰 원형 신호와 설명으로 전달한다. */
@Composable
fun EmergencyCallIndicator(
    title: String,
    description: String,
    isCalling: Boolean,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = if (isCalling) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.extraColors.warning
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .aspectRatio(1f)
                .sizeIn(
                    minWidth = HeartGuardComponentSize.TouchTarget,
                    minHeight = HeartGuardComponentSize.TouchTarget,
                    maxWidth = HeartGuardIconSize.EmergencyIndicatorMax,
                    maxHeight = HeartGuardIconSize.EmergencyIndicatorMax,
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                HeartGuardBorderWidth.EmergencyIndicator,
                indicatorColor.copy(alpha = 0.2f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(0.68f)
                        .aspectRatio(1f),
                    shape = CircleShape,
                    color = indicatorColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Figma 03_긴급상황·04_호출중 모두 원형 인디케이터 아이콘이 경고 삼각형으로 통일되어 있다.
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.fillMaxSize(0.48f),
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun EmergencyCallIndicatorPreview() {
    HeartGuardTheme {
        EmergencyCallIndicator(
            title = "관리자 호출 중",
            description = "잠시만 기다려 주세요",
            isCalling = true,
        )
    }
}
