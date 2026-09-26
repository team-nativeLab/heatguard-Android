package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardElevation
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 홈 날씨 지표(습도·체감온도·날씨)를 Figma의 아이콘 배지·세로 구분선·값 구조로 보여주는 흰 카드다. */
@Composable
fun HomeWeatherMetricsCard(
    humidityLabel: String,
    humidity: String,
    feelsLikeTemperatureLabel: String,
    feelsLikeTemperature: String,
    weatherLabel: String,
    weatherValue: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.HomeWeatherMetricHeight),
        shape = RoundedCornerShape(HeartGuardRadius.HomeMetric),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = HeartGuardElevation.HomeMetricCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HeartGuardSpacing.HomeMetricHorizontal,
                    vertical = HeartGuardSpacing.Section,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            HomeWeatherMetric(
                iconPainter = painterResource(R.drawable.home_humidity),
                metricLabel = humidityLabel,
                metricValue = humidity,
                modifier = Modifier.weight(1f),
            )

            HomeWeatherMetricDivider()

            HomeWeatherMetric(
                iconPainter = painterResource(R.drawable.home_feels_like),
                metricLabel = feelsLikeTemperatureLabel,
                metricValue = feelsLikeTemperature,
                modifier = Modifier.weight(1.25f),
            )

            HomeWeatherMetricDivider()

            HomeWeatherMetric(
                iconPainter = painterResource(R.drawable.home_weather),
                metricLabel = weatherLabel,
                metricValue = weatherValue,
                modifier = Modifier.weight(0.95f),
            )
        }
    }
}

@Composable
private fun HomeWeatherMetric(
    iconPainter: Painter,
    metricLabel: String,
    metricValue: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        Surface(
            modifier = Modifier.size(HeartGuardIconSize.HomeMetric),
            shape = RoundedCornerShape(HeartGuardRadius.Pill),
            color = MaterialTheme.extraColors.homeMetricContainer,
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.padding(HeartGuardSpacing.Compact),
            )
        }

        Column {
            Text(
                text = metricLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = metricValue,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun HomeWeatherMetricDivider() {
    Surface(
        modifier = Modifier
            .width(HeartGuardBorderWidth.Divider)
            .height(HeartGuardComponentSize.HomeMetricDividerHeight),
        color = MaterialTheme.colorScheme.outlineVariant,
    ) {}
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun HomeWeatherMetricsCardPreview() {
    HeartGuardTheme {
        HomeWeatherMetricsCard(
            humidityLabel = "습도",
            humidity = "55%",
            feelsLikeTemperatureLabel = "체감온도",
            feelsLikeTemperature = "40.5°C",
            weatherLabel = "날씨",
            weatherValue = "맑음",
        )
    }
}
