package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 온도 기록 화면 상단의 현재 온도·습도·체감온도를 Figma의 비카드형 Hero로 보여준다. */
@Composable
fun TemperatureSummaryCard(
    currentTemperature: String,
    humidity: String,
    feelsLikeTemperature: String,
    currentTemperatureLabel: String,
    humidityLabel: String,
    feelsLikeLabel: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") title: String,
    thermometerPainter: Painter? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HeartGuardSpacing.Item),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        if (thermometerPainter != null) {
            Image(
                painter = thermometerPainter,
                contentDescription = null,
                modifier = Modifier.size(HeartGuardIconSize.WeatherStatus),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            Text(
                text = currentTemperatureLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = currentTemperature,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = HeartGuardFontSize.TemperatureSummary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = stringResource(
                    R.string.home_weather_summary_format,
                    humidityLabel,
                    humidity,
                    feelsLikeLabel,
                    feelsLikeTemperature,
                ),
                color = MaterialTheme.extraColors.homeMutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun TemperatureSummaryCardPreview() {
    HeartGuardTheme {
        TemperatureSummaryCard(
            currentTemperature = "47.5°C",
            humidity = "55%",
            feelsLikeTemperature = "40.5°C",
            currentTemperatureLabel = "현재 온도",
            humidityLabel = "습도",
            feelsLikeLabel = "체감온도",
            title = stringResource(R.string.temperature_current_measurement),
            thermometerPainter = androidx.compose.ui.res.painterResource(R.drawable.record_temperature),
        )
    }
}
