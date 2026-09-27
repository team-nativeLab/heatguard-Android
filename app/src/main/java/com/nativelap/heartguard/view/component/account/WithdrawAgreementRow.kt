package com.nativelap.heartguard.view.component.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.control.HeartGuardCheckbox

/** 탈퇴 동의 체크박스와 문구다. 문구를 눌러도 체크가 바뀌며, 스크린리더에는 체크박스 하나로만 읽힌다. */
@Composable
fun WithdrawAgreementRow(
    agreementText: String,
    isAgreed: Boolean,
    onAgreementChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartGuardCheckbox(
            isChecked = isAgreed,
            onCheckedChange = onAgreementChange,
            contentDescription = agreementText,
        )

        Text(
            text = agreementText,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {}
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAgreementChange(!isAgreed) },
                ),
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 354)
@Composable
private fun WithdrawAgreementRowPreview() {
    HeartGuardTheme {
        WithdrawAgreementRow(
            agreementText = "유의사항을 모두 확인했으며, 탈퇴에 동의해요",
            isAgreed = true,
            onAgreementChange = {},
        )
    }
}
