package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 회원탈퇴처럼 되돌릴 수 없는 동작을 실행하는 빨간 버튼이다. 비활성일 때는 회색으로 표시한다. */
@Composable
fun WithdrawDangerButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
        shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.extraColors.disabledContent,
            disabledContentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WithdrawDangerButtonPreview() {
    HeartGuardTheme {
        WithdrawDangerButton(
            title = "탈퇴하기",
            onClick = {},
        )
    }
}
