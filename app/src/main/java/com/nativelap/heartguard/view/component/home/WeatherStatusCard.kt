package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 홈 화면에서 현장 온도, 폭염 위험도, 습도와 체감온도를 함께 보여준다. */
@Composable
fun WeatherStatusCard(
    weatherPainter: Painter,
    weatherContentDescription: String,
    statusTitle: String,
    currentTemperature: String,
    feelsLikeTemperature: String,
    feelsLikeTemperatureLabel: String,
    humidity: String,
    humidityLabel: String,
    weatherValue: String,
    weatherLabel: String,
    temperatureDeltaLabel: String,
    temperatureDelta: String,
    riskLabel: String,
    isTemperatureIncreasing: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.WeatherCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.WeatherCardMinHeight)
                .padding(HeartGuardSpacing.Section),
        ) {
            if (maxWidth < HeartGuardComponentSize.WeatherWideLayoutBreakpoint) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                    ) {
                        WeatherStatusSummary(
                            statusTitle = statusTitle,
                            currentTemperature = currentTemperature,
                            temperatureDeltaLabel = temperatureDeltaLabel,
                            temperatureDelta = temperatureDelta,
                            riskLabel = riskLabel,
                            isTemperatureIncreasing = isTemperatureIncreasing,
                            modifier = Modifier.weight(1f),
                        )
                        WeatherIllustration(
                            weatherPainter = weatherPainter,
                            weatherContentDescription = weatherContentDescription,
                            modifier = Modifier.weight(0.42f),
                        )
                    }
                    WeatherSummaryLine(
                        humidityLabel = humidityLabel,
                        humidity = humidity,
                        feelsLikeTemperatureLabel = feelsLikeTemperatureLabel,
                        feelsLikeTemperature = feelsLikeTemperature,
                    )
                    WeatherMetrics(
                        feelsLikeTemperature = feelsLikeTemperature,
                        feelsLikeTemperatureLabel = feelsLikeTemperatureLabel,
                        humidity = humidity,
                        humidityLabel = humidityLabel,
                        weatherValue = weatherValue,
                        weatherLabel = weatherLabel,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                    ) {
                        WeatherStatusSummary(
                            statusTitle = statusTitle,
                            currentTemperature = currentTemperature,
                            temperatureDeltaLabel = temperatureDeltaLabel,
                            temperatureDelta = temperatureDelta,
                            riskLabel = riskLabel,
                            isTemperatureIncreasing = isTemperatureIncreasing,
                            modifier = Modifier
                                .weight(1f),
                        )
                        WeatherIllustration(
                            weatherPainter = weatherPainter,
                            weatherContentDescription = weatherContentDescription,
                            modifier = Modifier
                                .weight(0.68f),
                        )
                    }
                    WeatherSummaryLine(
                        humidityLabel = humidityLabel,
                        humidity = humidity,
                        feelsLikeTemperatureLabel = feelsLikeTemperatureLabel,
                        feelsLikeTemperature = feelsLikeTemperature,
                    )
                    WeatherMetrics(
                        feelsLikeTemperature = feelsLikeTemperature,
                        feelsLikeTemperatureLabel = feelsLikeTemperatureLabel,
                        humidity = humidity,
                        humidityLabel = humidityLabel,
                        weatherValue = weatherValue,
                        weatherLabel = weatherLabel,
                    )
                }
            }
        }
    }
}

/** 날씨 일러스트가 부모 폭을 초과하지 않도록 최대 크기와 정사각 비율을 함께 적용한다. */
@Composable
private fun WeatherIllustration(
    weatherPainter: Painter,
    weatherContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = weatherPainter,
        contentDescription = weatherContentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .aspectRatio(1f)
            .sizeIn(maxWidth = HeartGuardIconSize.WeatherIllustrationMax),
    )
}

/** 날씨 카드의 위험도·현재 온도·온도 변화 영역을 폭에 맞춰 배치한다. */
@Composable
private fun WeatherStatusSummary(
    statusTitle: String,
    currentTemperature: String,
    temperatureDeltaLabel: String,
    temperatureDelta: String,
    riskLabel: String,
    isTemperatureIncreasing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
    ) {
        HeatRiskBadge(riskLabel = riskLabel)
        Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
        Text(
            text = statusTitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = currentTemperature,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = HeartGuardFontSize.HeroTemperature,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            TemperatureDelta(
                deltaLabel = temperatureDeltaLabel,
                deltaValue = temperatureDelta,
                isIncreasing = isTemperatureIncreasing,
                modifier = Modifier.padding(bottom = HeartGuardSpacing.Tight),
            )
        }
    }
}

/**
 * Figma 02_홈_리디자인: 온도 강조 영역 아래 "습도 55% · 체감온도 40.5°C" 형태의
 * 한 줄 요약을 보여준다.
 *
 * TODO: " · " 구분자와 "라벨 값" 결합 방식을 strings.xml의 format string
 *  (예: home_weather_summary_format="%1$s %2$s · %3$s %4$s")으로 옮긴다.
 */
@Composable
private fun WeatherSummaryLine(
    humidityLabel: String,
    humidity: String,
    feelsLikeTemperatureLabel: String,
    feelsLikeTemperature: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "$humidityLabel $humidity · $feelsLikeTemperatureLabel $feelsLikeTemperature",
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** 세부 날씨 지표를 넓은 화면에서는 3열, 좁은 화면에서는 세로로 재배치한다. */
@Composable
private fun WeatherMetrics(
    feelsLikeTemperature: String,
    feelsLikeTemperatureLabel: String,
    humidity: String,
    humidityLabel: String,
    weatherValue: String,
    weatherLabel: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (
                maxWidth < HeartGuardComponentSize.MetricThreeColumnBreakpoint
            ) {
                1
            } else {
                3
            },
            horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            WeatherMetric(
                metricLabel = humidityLabel,
                metricValue = humidity,
                modifier = Modifier.weight(1f),
            )
            WeatherMetric(
                metricLabel = feelsLikeTemperatureLabel,
                metricValue = feelsLikeTemperature,
                modifier = Modifier.weight(1f),
            )
            WeatherMetric(
                metricLabel = weatherLabel,
                metricValue = weatherValue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun WeatherStatusCardPreview() {
    HeartGuardTheme {
        WeatherStatusCard(
            weatherPainter = androidx.compose.ui.res.painterResource(R.drawable.weather_sunny),
            weatherContentDescription = "맑음",
            statusTitle = "현재 현장 날씨",
            currentTemperature = "37°C",
            feelsLikeTemperature = "40°C",
            feelsLikeTemperatureLabel = "체감온도",
            humidity = "65%",
            humidityLabel = "습도",
            weatherValue = "맑음",
            weatherLabel = "날씨",
            temperatureDeltaLabel = "온도 변화",
            temperatureDelta = "+2°C",
            riskLabel = "주의",
            isTemperatureIncreasing = true,
        )
    }
}
