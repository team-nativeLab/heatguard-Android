package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

/** 탈퇴 완료 화면의 파란 체크 배지·제목·감사 문구를 담은 흰 결과 카드다. */
@Composable
fun WithdrawDoneMessage(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.ResultMessageHeight),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = HeartGuardSpacing.Section,
                vertical = HeartGuardSpacing.ResultMessageTopBottom,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.Result)
                    .background(
                        color = MaterialTheme.extraColors.photoContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(HeartGuardIconSize.ResultGlyph),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = title,
                modifier = Modifier.padding(top = HeartGuardSpacing.Item),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = HeartGuardFontSize.ResultMessageTitle,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = description,
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = HeartGuardFontSize.ResultMessageDescription,
                    fontWeight = FontWeight.Medium,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF9FBFC, widthDp = 352)
@Composable
private fun WithdrawDoneMessagePreview() {
    HeartGuardTheme {
        WithdrawDoneMessage(
            title = "탈퇴가 완료되었어요",
            description = "그동안 폭염가드를 이용해주셔서 감사해요",
        )
    }
}
