package com.nativelap.heartguard.view.component.password

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.auth.AuthTextField

/** 보기 토글(눈 아이콘)이 있는 비밀번호 입력칸이다(Figma 26~28). 오류 문구는 입력칸 아래 별도 안내로 보여주므로 여기서는 테두리만 바꾼다. */
@Composable
fun PasswordInputField(
    label: String,
    password: String,
    placeholder: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    AuthTextField(
        label = label,
        text = password,
        onTextChange = onPasswordChange,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        // Figma 27: 비밀번호 변경 오류는 배경을 유지하고 빨간 테두리만 표시한다.
        errorContainerColor = MaterialTheme.extraColors.authInputBackground,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        visualTransformation =
            if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            IconButton(onClick = onVisibilityClick) {
                Icon(
                    imageVector =
                        if (isPasswordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                    contentDescription =
                        if (isPasswordVisible) {
                            stringResource(R.string.password_hide)
                        } else {
                            stringResource(R.string.password_show)
                        },
                    tint = MaterialTheme.extraColors.tertiaryText,
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PasswordInputFieldPreview() {
    HeartGuardTheme {
        PasswordInputField(
            label = "새 비밀번호",
            password = "abc12345",
            placeholder = "새 비밀번호 입력",
            isPasswordVisible = false,
            onPasswordChange = {},
            onVisibilityClick = {},
        )
    }
}
