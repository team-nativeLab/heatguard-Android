package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 인증 화면의 안내 문구와 후속 행동 링크를 한 줄로 표현한다. */
@Composable
fun AuthPrompt(
    message: String,
    actionTitle: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = MaterialTheme.extraColors.authOnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = HeartGuardFontSize.AuthPrompt,
            ),
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionTitle,
                color = MaterialTheme.extraColors.authPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = HeartGuardFontSize.AuthPrompt,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthPromptPreview() {
    HeartGuardTheme {
        AuthPrompt(
            message = "아직 회원이 아니신가요?",
            actionTitle = "회원가입",
            onActionClick = {},
        )
    }
}
