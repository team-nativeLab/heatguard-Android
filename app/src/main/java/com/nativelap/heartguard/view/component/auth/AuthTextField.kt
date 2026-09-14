package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

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
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

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
                .heightIn(min = HeartGuardComponentSize.TextFieldHeight)
                // 포커스를 얻은 직후 IME를 명시적으로 열어 에뮬레이터에서도 입력을 보장한다.
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        softwareKeyboardController?.show()
                    }
                },
            placeholder = { Text(text = placeholder) },
            singleLine = true,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(HeartGuardRadius.Button),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                errorContainerColor = MaterialTheme.extraColors.alertContainer,
                // Figma 01_로그인·회원가입: 입력 필드는 테두리 없이 배경색만으로 구분된다.
                focusedBorderColor = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Transparent
                },
                unfocusedBorderColor = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.Transparent
                },
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            supportingText = supportingText?.let { message ->
                {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
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
