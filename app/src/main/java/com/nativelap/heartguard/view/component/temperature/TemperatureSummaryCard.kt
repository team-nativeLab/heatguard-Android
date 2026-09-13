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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 온도 기록 화면 상단에서 현재 온도·습도·체감온도를 요약한다. */
@Composable
fun TemperatureSummaryCard(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    currentTemperatureLabel: String,
    humidityLabel: String,
    feelsLikeLabel: String,
    modifier: Modifier = Modifier,
    title: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                TemperatureSummaryMetric(
                    label = currentTemperatureLabel,
                    value = currentTemperature,
                    modifier = Modifier.weight(1f),
                )
                TemperatureSummaryMetric(
                    label = humidityLabel,
                    value = humidity,
                    modifier = Modifier.weight(1f),
                )
                TemperatureSummaryMetric(
                    label = feelsLikeLabel,
                    value = feelsLikeTemperature,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TemperatureSummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = HeartGuardFontSize.TemperatureSummary,
                fontWeight = FontWeight.ExtraBold,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureSummaryCardPreview() {
    HeartGuardTheme {
        TemperatureSummaryCard(
            currentTemperature = "37℃",
            humidity = "65%",
            feelsLikeTemperature = "40℃",
            currentTemperatureLabel = "현재 온도",
            humidityLabel = "습도",
            feelsLikeLabel = "체감온도",
            title = stringResource(R.string.temperature_current_measurement),
        )
    }
}
