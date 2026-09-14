package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 온도계 데이터 직접 입력 체크와 온도·습도·체감온도 입력값을 같은 카드에 표시한다. */
@Composable
fun TemperatureRecordCard(
    temperatureLabel: String,
    temperatureText: String,
    temperatureUnit: String,
    onTemperatureChange: (String) -> Unit,
    humidityLabel: String,
    humidityText: String,
    humidityUnit: String,
    onHumidityChange: (String) -> Unit,
    feelsLikeLabel: String,
    feelsLikeText: String,
    installationLabel: String,
    isManualInputEnabled: Boolean,
    onManualInputChange: (Boolean) -> Unit,
    checkboxContentDescription: String,
    modifier: Modifier = Modifier,
    title: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )

            TemperatureCheckboxRow(
                label = installationLabel,
                isChecked = isManualInputEnabled,
                onCheckedChange = onManualInputChange,
                checkboxContentDescription = checkboxContentDescription,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                TemperatureInputField(
                    label = temperatureLabel,
                    temperatureText = temperatureText,
                    unitLabel = temperatureUnit,
                    onTemperatureChange = onTemperatureChange,
                    isEnabled = isManualInputEnabled,
                    modifier = Modifier.weight(1f),
                )
                TemperatureInputField(
                    label = humidityLabel,
                    temperatureText = humidityText,
                    unitLabel = humidityUnit,
                    onTemperatureChange = onHumidityChange,
                    isEnabled = isManualInputEnabled,
                    modifier = Modifier.weight(1f),
                )
            }

            TemperatureCalculatedValue(
                label = feelsLikeLabel,
                value = feelsLikeText,
                isEnabled = isManualInputEnabled,
            )
        }
    }
}

/** 직접 입력된 값으로 계산되는 체감온도를 읽기 전용 필드처럼 표시한다. */
@Composable
private fun TemperatureCalculatedValue(
    label: String,
    value: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            color = if (isEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureRecordCardPreview() {
    HeartGuardTheme {
        TemperatureRecordCard(
            temperatureLabel = "온도",
            temperatureText = "47.5",
            temperatureUnit = "°C",
            onTemperatureChange = {},
            humidityLabel = "습도",
            humidityText = "55",
            humidityUnit = "%",
            onHumidityChange = {},
            feelsLikeLabel = "체감온도",
            feelsLikeText = "자동 계산",
            installationLabel = "온도계 데이터 입력 (온도계 미설치 시 체크)",
            isManualInputEnabled = true,
            onManualInputChange = {},
            checkboxContentDescription = "온도계 데이터 직접 입력",
            title = "온도계 데이터 입력",
        )
    }
}
