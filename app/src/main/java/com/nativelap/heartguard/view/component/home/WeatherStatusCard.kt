package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 홈 상단의 폭염 단계 배지·현재 온도 Hero와 그 아래 날씨 지표 카드를 Figma 홈 리디자인 순서로 구성한다.
 * 화면 좌우 여백은 이 컴포넌트가 직접 가지므로 호출부는 가로 padding을 주지 않는다. */
@Composable
fun WeatherStatusCard(
    weatherPainter: Painter,
    weatherContentDescription: String?,
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
    isTemperatureIncreasing: Boolean?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < COMPACT_HERO_LAYOUT_WIDTH ||
            LocalDensity.current.fontScale >= LARGE_FONT_SCALE_BREAKPOINT
        val heroStartInset = if (isCompact) {
            HeartGuardSpacing.ScreenHorizontal
        } else {
            HeartGuardSpacing.HomeHeroHorizontal
        }
        val illustrationWidth = if (isCompact) {
            COMPACT_ILLUSTRATION_WIDTH
        } else {
            HeartGuardComponentSize.HomeWeatherIllustrationWidth
        }
        val illustrationHeight = if (isCompact) {
            COMPACT_ILLUSTRATION_HEIGHT
        } else {
            HeartGuardComponentSize.HomeWeatherIllustrationHeight
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = heroStartInset,
                        end = HeartGuardSpacing.HomeHeroIllustrationEnd,
                    ),
            ) {
                // 실제 폰트 지표에서는 현재 온도와 변화량 배지 폭이 겹칠 수 있어 날씨 일러스트를 먼저 그린다.
                Image(
                    painter = weatherPainter,
                    contentDescription = weatherContentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(width = illustrationWidth, height = illustrationHeight),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    HeatRiskBadge(riskLabel = riskLabel)

                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

                    Text(
                        text = statusTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                    ) {
                        Text(
                            text = currentTemperature,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = if (isCompact) {
                                    HeartGuardFontSize.HeroTemperatureCompact
                                } else {
                                    HeartGuardFontSize.HeroTemperature
                                },
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        TemperatureDelta(
                            deltaLabel = temperatureDeltaLabel,
                            deltaValue = temperatureDelta,
                            isIncreasing = isTemperatureIncreasing,
                        )
                    }

                    Text(
                        text = stringResource(
                            R.string.home_weather_summary_format,
                            humidityLabel,
                            humidity,
                            feelsLikeTemperatureLabel,
                            feelsLikeTemperature,
                        ),
                        color = MaterialTheme.extraColors.homeMutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(HeartGuardSpacing.LargeSection))

            HomeWeatherMetricsCard(
                humidityLabel = humidityLabel,
                humidity = humidity,
                feelsLikeTemperatureLabel = feelsLikeTemperatureLabel,
                feelsLikeTemperature = feelsLikeTemperature,
                weatherLabel = weatherLabel,
                weatherValue = weatherValue,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeMetricCardHorizontal),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun WeatherStatusCardPreview() {
    HeartGuardTheme {
        WeatherStatusCard(
            weatherPainter = painterResource(R.drawable.heartguard_home_weather),
            weatherContentDescription = "맑음",
            statusTitle = "현재 온도",
            currentTemperature = "47.5°C",
            feelsLikeTemperature = "40.5°C",
            feelsLikeTemperatureLabel = "체감온도",
            humidity = "55%",
            humidityLabel = "습도",
            weatherValue = "맑음",
            weatherLabel = "날씨",
            temperatureDeltaLabel = "온도 변화",
            temperatureDelta = "+3.2°C",
            riskLabel = "폭염 주의 단계",
            isTemperatureIncreasing = true,
        )
    }
}

private val COMPACT_HERO_LAYOUT_WIDTH = 380.dp
private val COMPACT_ILLUSTRATION_WIDTH = 120.dp
private val COMPACT_ILLUSTRATION_HEIGHT = 92.dp
private const val LARGE_FONT_SCALE_BREAKPOINT = 1.5f
