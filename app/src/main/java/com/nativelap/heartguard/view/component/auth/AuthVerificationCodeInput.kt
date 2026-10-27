package com.nativelap.heartguard.view.component.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

@Composable
internal fun AuthVerificationCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val accessibilityDescription =
        stringResource(R.string.auth_signup_code_accessibility) + ", " +
            stringResource(R.string.auth_signup_code_digit_count, code.length)

    BasicTextField(
        value = code,
        onValueChange = { input ->
            onCodeChange(input.filter { digit -> digit in '0'..'9' }.take(CODE_LENGTH))
        },
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .semantics { contentDescription = accessibilityDescription },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
        cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
        visualTransformation = VisualTransformation.None,
        decorationBox = { innerTextField ->
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cellWidth = minOf(CODE_CELL_WIDTH, (maxWidth - CODE_CELL_GAP * (CODE_LENGTH - 1)) / CODE_LENGTH)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(CODE_LENGTH) { index ->
                        val isCurrentCell = isFocused && index == code.length && code.length < CODE_LENGTH
                        val hasDigit = index < code.length
                        Box(
                            modifier =
                                Modifier
                                    .width(cellWidth)
                                    .height(CODE_CELL_HEIGHT)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(CODE_CELL_CORNER_RADIUS),
                                    ).border(
                                        width = if (isCurrentCell) 2.dp else 0.dp,
                                        color =
                                            if (isCurrentCell) {
                                                MaterialTheme.extraColors.authPrimary
                                            } else {
                                                androidx.compose.ui.graphics.Color.Transparent
                                            },
                                        shape = RoundedCornerShape(CODE_CELL_CORNER_RADIUS),
                                    ).padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasDigit) {
                                Text(
                                    text = code[index].toString(),
                                    color = MaterialTheme.extraColors.authOnSurface,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            } else if (isCurrentCell) {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(CODE_CURSOR_WIDTH)
                                            .height(CODE_CURSOR_HEIGHT)
                                            .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.matchParentSize()) {
                    innerTextField()
                }
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 354)
@Composable
private fun AuthVerificationCodeInputPreview() {
    HeartGuardTheme {
        AuthVerificationCodeInput(
            code = "123",
            onCodeChange = {},
            enabled = true,
        )
    }
}

private const val CODE_LENGTH = 6
private val CODE_CELL_WIDTH = 48.dp
private val CODE_CELL_HEIGHT = 56.dp
private val CODE_CELL_GAP = 13.dp
private val CODE_CELL_CORNER_RADIUS = 12.dp
private val CODE_CURSOR_WIDTH = 2.dp
private val CODE_CURSOR_HEIGHT = 22.dp
