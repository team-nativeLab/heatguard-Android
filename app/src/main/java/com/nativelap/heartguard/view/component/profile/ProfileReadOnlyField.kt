package com.nativelap.heartguard.view.component.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 내 정보 수정 화면에서 편집할 수 없는 값(회사명·이메일)을 입력칸 모양으로 보여준다.
 * [value]가 null이면 서버가 제공하지 않은 값이므로 "--"를 표시한다. */
@Composable
fun ProfileReadOnlyField(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Compact),
            shape = RoundedCornerShape(HeartGuardRadius.InputBox),
            color = MaterialTheme.extraColors.authInputBackground,
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = HeartGuardComponentSize.TextFieldHeight)
                    .padding(horizontal = HeartGuardSpacing.Item),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value ?: stringResource(R.string.common_empty_value),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        color = MaterialTheme.extraColors.tertiaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = HeartGuardSpacing.Tight),
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileReadOnlyFieldPreview() {
    HeartGuardTheme {
        ProfileReadOnlyField(
            label = "이메일",
            value = "worker01",
            trailingText = "변경 불가",
        )
    }
}
