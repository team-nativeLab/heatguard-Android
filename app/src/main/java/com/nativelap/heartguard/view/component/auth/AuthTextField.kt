package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 로그인 화면에서 라벨·입력값·오류 안내를 함께 표시하는 입력 Component다. */
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
    // 비밀번호 보기 토글처럼 입력칸 오른쪽에 두는 아이콘이다. 없으면 표시하지 않는다.
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val inputViewportRequester = remember { BringIntoViewRequester() }
    var isInputFocused by remember { mutableStateOf(false) }
    val keyboardBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(isInputFocused, keyboardBottom) {
        if (isInputFocused && keyboardBottom > 0) {
            inputViewportRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.authOnSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = HeartGuardFontSize.AuthLabel,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        val textFieldInteractionSource = remember {
            MutableInteractionSource()
        }
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
            unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
            errorContainerColor = MaterialTheme.extraColors.alertContainer,
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
            focusedTextColor = MaterialTheme.extraColors.authOnSurface,
            unfocusedTextColor = MaterialTheme.extraColors.authOnSurface,
            focusedPlaceholderColor = MaterialTheme.extraColors.authOnSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.extraColors.authOnSurfaceVariant,
            errorPlaceholderColor = MaterialTheme.extraColors.authOnSurfaceVariant,
        )

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.AuthFieldLabelInput)
                .heightIn(min = HeartGuardComponentSize.AuthTextFieldHeight)
                .bringIntoViewRequester(inputViewportRequester)
                .semantics {
                    if (isError && supportingText != null) {
                        error(supportingText)
                    }
                }
                // 포커스를 얻은 직후 IME를 명시적으로 열어 에뮬레이터에서도 입력을 보장한다.
                .onFocusChanged { focusState ->
                    isInputFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        softwareKeyboardController?.show()
                    }
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = HeartGuardFontSize.AuthInput,
            ),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            interactionSource = textFieldInteractionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    interactionSource = textFieldInteractionSource,
                    isError = isError,
                    trailingIcon = trailingIcon,
                    placeholder = {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = HeartGuardFontSize.AuthInput,
                            ),
                        )
                    },
                    colors = textFieldColors,
                    contentPadding = PaddingValues(
                        horizontal = HeartGuardSpacing.ScreenHorizontal,
                    ),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = isError,
                            interactionSource = textFieldInteractionSource,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(HeartGuardRadius.Button),
                            focusedBorderThickness = if (isError) 2.dp else 0.dp,
                            unfocusedBorderThickness = if (isError) 1.dp else 0.dp,
                        )
                    },
                )
            },
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
