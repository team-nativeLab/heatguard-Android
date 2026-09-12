package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.VisualTransformation
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** 로그인과 회원가입에서 사용하는 라벨-입력-오류 메시지 묶음이다. */
@Composable
fun AuthTextField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Compact)
                .heightIn(min = HeartGuardComponentSize.TextFieldHeight),
            placeholder = { Text(text = placeholder) },
            singleLine = true,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(HeartGuardRadius.Button),
            supportingText = supportingText?.let { message ->
                { Text(text = message) }
            },
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun AuthTextFieldPreview() {
    HeartGuardTheme {
        AuthTextField(
            label = "이메일",
            text = "",
            onTextChange = {},
            placeholder = "이메일을 입력해 주세요",
        )
    }
}
