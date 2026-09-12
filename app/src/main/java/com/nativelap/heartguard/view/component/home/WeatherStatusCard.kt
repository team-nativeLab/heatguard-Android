package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 홈의 현재 현장 날씨 정보를 하나의 재사용 가능한 카드 역할로 제공한다. */
@Composable
fun WeatherStatusCard(
    weatherPainter: Painter,
    weatherContentDescription: String,
    statusTitle: String,
    currentTemperature: String,
    currentTemperatureLabel: String,
    feelsLikeTemperature: String,
    feelsLikeTemperatureLabel: String,
    humidity: String,
    humidityLabel: String,
    temperatureDeltaLabel: String,
    temperatureDelta: String,
    riskLabel: String,
    isTemperatureIncreasing: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.WeatherCardMinHeight),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                WeatherIcon(
                    weatherPainter = weatherPainter,
                    contentDescription = weatherContentDescription,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    TemperatureDelta(
                        deltaLabel = temperatureDeltaLabel,
                        deltaValue = temperatureDelta,
                        isIncreasing = isTemperatureIncreasing,
                    )
                }
                HeatRiskBadge(riskLabel = riskLabel)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                WeatherMetric(
                    metricLabel = currentTemperatureLabel,
                    metricValue = currentTemperature,
                    modifier = Modifier.weight(1f),
                )
                WeatherMetric(
                    metricLabel = feelsLikeTemperatureLabel,
                    metricValue = feelsLikeTemperature,
                    modifier = Modifier.weight(1f),
                )
                WeatherMetric(
                    metricLabel = humidityLabel,
                    metricValue = humidity,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun WeatherStatusCardPreview() {
    HeartGuardTheme {
        WeatherStatusCard(
            weatherPainter = androidx.compose.ui.res.painterResource(
                com.nativelap.heartguard.R.drawable.weather_sunny,
            ),
            weatherContentDescription = "맑음",
            statusTitle = "현재 현장 날씨",
            currentTemperature = "37℃",
            currentTemperatureLabel = "현재 온도",
            feelsLikeTemperature = "40℃",
            feelsLikeTemperatureLabel = "체감온도",
            humidity = "65%",
            humidityLabel = "습도",
            temperatureDeltaLabel = "온도 변화",
            temperatureDelta = "+2℃",
            riskLabel = "주의",
            isTemperatureIncreasing = true,
        )
    }
}
