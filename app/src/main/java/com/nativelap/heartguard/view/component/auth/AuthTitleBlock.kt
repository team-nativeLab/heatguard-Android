package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 로그인·회원가입 화면의 큰 제목과 보조 설명을 하나의 수직 블록으로 제공한다. */
@Composable
fun AuthTitleBlock(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Figma 01_로그인·회원가입: 브랜드 로고는 중앙, 제목/설명은 왼쪽 정렬로 배치된다.
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = MaterialTheme.extraColors.authOnSurface,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = HeartGuardFontSize.AuthTitle,
                lineHeight = HeartGuardFontSize.AuthTitleLineHeight,
                letterSpacing = HeartGuardFontSize.AuthTitleLetterSpacing,
            ),
        )
        Text(
            text = description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.AuthTitleDescription),
            textAlign = TextAlign.Start,
            color = MaterialTheme.extraColors.authOnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = HeartGuardFontSize.AuthDescription,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun AuthTitleBlockPreview() {
    HeartGuardTheme {
        AuthTitleBlock(
            title = "폭염가드 로그인",
            description = "현장 안전 관리를 시작해 보세요",
        )
    }
}
