package com.nativelap.heartguard.view.component.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 드로어 맨 아래의 회원탈퇴 진입 링크다. 실수로 누르지 않도록 Figma처럼 옅은 밑줄 텍스트로 낮게 강조한다. */
@Composable
fun MenuDrawerWithdrawLink(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = HeartGuardComponentSize.TouchTarget)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            color = MaterialTheme.extraColors.tertiaryText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = HeartGuardFontSize.SmallLabel,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuDrawerWithdrawLinkPreview() {
    HeartGuardTheme {
        MenuDrawerWithdrawLink(
            title = "회원탈퇴",
            onClick = {},
        )
    }
}
