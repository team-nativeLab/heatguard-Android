package com.nativelap.heartguard.view.component.temperature

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 온도 기록 입력 화면의 숫자 입력과 단위 표시를 함께 제공한다. */
@Composable
fun TemperatureInputField(
    label: String,
    temperatureText: String,
    unitLabel: String,
    onTemperatureChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        OutlinedTextField(
            value = temperatureText,
            onValueChange = onTemperatureChange,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Compact)
                .heightIn(min = HeartGuardComponentSize.TextFieldHeight),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                Text(
                    text = unitLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = RoundedCornerShape(HeartGuardRadius.Button),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureInputFieldPreview() {
    HeartGuardTheme {
        TemperatureInputField(
            label = "현재 온도",
            temperatureText = "37",
            unitLabel = "℃",
            onTemperatureChange = {},
        )
    }
}
