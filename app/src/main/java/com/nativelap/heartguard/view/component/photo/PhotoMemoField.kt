package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 작업·휴식 사진에 남길 메모를 디자인의 다줄 입력 필드로 제공한다. */
@Composable
fun PhotoMemoField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    contentHorizontalPadding: Dp = 0.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            if (isOptional) {
                Text(
                    text = stringResource(R.string.common_optional_parenthesized),
                    color = MaterialTheme.extraColors.homeMutedText,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = HeartGuardFontSize.SmallLabel,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Compact)
                    .padding(horizontal = contentHorizontalPadding)
                    .heightIn(min = HeartGuardComponentSize.PhotoMemoMinHeight),
            placeholder = {
                Text(
                    text = placeholder,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = HeartGuardFontSize.SmallLabel,
                            fontWeight = FontWeight.Medium,
                        ),
                )
            },
            minLines = 3,
            shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.extraColors.cardBorder,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedPlaceholderColor = MaterialTheme.extraColors.tertiaryText,
                    focusedPlaceholderColor = MaterialTheme.extraColors.tertiaryText,
                ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PhotoMemoFieldPreview() {
    HeartGuardTheme {
        PhotoMemoField(
            label = "메모",
            text = "",
            onTextChange = {},
            placeholder = "메모를 입력해 주세요",
            isOptional = true,
        )
    }
}
