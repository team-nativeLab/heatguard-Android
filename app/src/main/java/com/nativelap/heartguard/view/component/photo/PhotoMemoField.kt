package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
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
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isOptional) {
                "$label (${stringResource(R.string.common_optional)})"
            } else {
                label
            },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Compact)
                .heightIn(min = HeartGuardComponentSize.PhotoMemoMinHeight),
            placeholder = { Text(text = placeholder) },
            minLines = 3,
            shape = RoundedCornerShape(HeartGuardRadius.Button),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
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
