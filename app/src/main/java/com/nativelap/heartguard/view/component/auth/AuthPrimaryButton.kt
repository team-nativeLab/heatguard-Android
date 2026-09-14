package com.nativelap.heartguard.view.component.auth

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

/** 인증 흐름에서 가장 중요한 제출 동작을 나타내는 기본 버튼이다. */
@Composable
fun AuthPrimaryButton(
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
            .heightIn(min = HeartGuardComponentSize.AuthButtonHeight),
        shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.extraColors.authPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.extraColors.disabledContent,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthPrimaryButtonPreview() {
    HeartGuardTheme {
        AuthPrimaryButton(title = "로그인", onClick = {})
    }
}
