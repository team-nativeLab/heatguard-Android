package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 온도 카드 안에서 숫자와 자동 계산값을 동일한 입력 박스 스타일로 보여준다. */
@Composable
fun TemperatureInputField(
    label: String,
    temperatureText: String,
    unitLabel: String,
    onTemperatureChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    placeholderText: String = "",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
        OutlinedTextField(
            value = temperatureText,
            onValueChange = onTemperatureChange,
            enabled = isEnabled,
            // 이 String 기반 OutlinedTextField 오버로드는 contentPadding을 노출하지 않아
            // 기본 상하 padding(16dp)을 줄일 수 없으므로, HeartGuardComponentSize.CompactInputHeight
            // (Figma InputBox 실측 40dp) 고정 높이를 강제하지 않는다 — 강제하면 텍스트가 위아래로
            // 잘린다. bodySmall 텍스트 스타일로 필드가 필요한 최소 높이만큼만 자라도록 둔다.
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Tight),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = if (unitLabel.isNotEmpty()) {
                {
                    Text(
                        text = unitLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                null
            },
            shape = RoundedCornerShape(HeartGuardRadius.Button),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                disabledContainerColor = MaterialTheme.extraColors.authInputBackground,
                unfocusedBorderColor = MaterialTheme.extraColors.authInputBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                disabledBorderColor = MaterialTheme.extraColors.authInputBackground,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureInputFieldPreview() {
    HeartGuardTheme {
        TemperatureInputField(
            label = "온도 (°C)",
            temperatureText = "",
            unitLabel = "",
            onTemperatureChange = {},
            placeholderText = "예: 47.5",
        )
    }
}
