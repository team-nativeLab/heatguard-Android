package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 회원탈퇴 안내 화면 상단의 큰 제목과 보조 설명이다. */
@Composable
fun WithdrawHeadline(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
    ) {
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = HeartGuardFontSize.WithdrawTitle,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = subtitle,
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 354)
@Composable
private fun WithdrawHeadlinePreview() {
    HeartGuardTheme {
        WithdrawHeadline(
            title = "탈퇴하기 전에 꼭 확인해주세요",
            subtitle = "탈퇴 후에는 아래 내용이 적용돼요",
        )
    }
}
