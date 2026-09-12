package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.R

/** 선택 상태에 따라 활성화 여부가 달라지는 기록 유형 확인 버튼이다. */
@Composable
fun RecordTypeConfirmButton(
    title: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
        shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
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
private fun RecordTypeConfirmButtonPreview() {
    HeartGuardTheme {
        RecordTypeConfirmButton(
            title = stringResource(R.string.common_confirm),
            isEnabled = true,
            onClick = {},
        )
    }
}
