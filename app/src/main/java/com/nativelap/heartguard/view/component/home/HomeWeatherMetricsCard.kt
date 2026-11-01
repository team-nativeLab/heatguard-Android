package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 홈 날씨 지표를 Figma의 아이콘 배지, 가로 그룹, 값 구조로 보여주는 흰 카드다. */
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
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.HomeWeatherMetricHeight),
        shape = RoundedCornerShape(HeartGuardRadius.HomeMetric),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = HOME_METRIC_FIGMA_SHADOW_ELEVATION,
    ) {
        // 폭이 좁거나 글자가 커지면 여백과 아이콘을 줄이고, 필요한 경우 가로 스크롤을 허용한다.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val regularHorizontalPadding = HeartGuardSpacing.HomeMetricHorizontal
            val compactHorizontalPadding = HeartGuardSpacing.Compact
            val regularIconValueSpacing = HeartGuardSpacing.Compact
            val compactIconValueSpacing = HeartGuardSpacing.Tight
            val regularGroupSpacing = HeartGuardSpacing.Compact
            val compactGroupSpacing = HeartGuardSpacing.Tight
            val regularIconSize = HeartGuardIconSize.HomeMetric
            val humidityValueColumnWidth = HomeHumidityValueColumnMinWidth * fontScale
            val feelsLikeValueColumnWidth = HomeFeelsLikeValueColumnMinWidth * fontScale
            val weatherValueColumnWidth = HomeWeatherValueColumnMinWidth * fontScale
            val regularMinimumGroupSpacing = HeartGuardSpacing.Compact
            val regularMinimumContentWidth =
                metricRowMinimumWidth(
                    iconSize = regularIconSize,
                    iconValueSpacing = regularIconValueSpacing,
                    groupSpacing = regularMinimumGroupSpacing,
                    humidityValueColumnWidth = humidityValueColumnWidth,
                    feelsLikeValueColumnWidth = feelsLikeValueColumnWidth,
                    weatherValueColumnWidth = weatherValueColumnWidth,
                )
            val regularContentWidth = maxWidth - regularHorizontalPadding * 2
            val widthDeficit = (regularMinimumContentWidth - regularContentWidth).coerceAtLeast(0.dp)
            val horizontalPadding =
                if (widthDeficit > 0.dp) {
                    (regularHorizontalPadding - widthDeficit / 2)
                        .coerceAtLeast(compactHorizontalPadding)
                } else {
                    regularHorizontalPadding
                }
            val availableContentWidth = maxWidth - horizontalPadding * 2
            val remainingWidthDeficit =
                (regularMinimumContentWidth - availableContentWidth).coerceAtLeast(0.dp)
            val iconValueSpacing =
                if (remainingWidthDeficit > 0.dp) {
                    compactIconValueSpacing
                } else {
                    regularIconValueSpacing
                }
            val compactIconMinimumContentWidth =
                metricRowMinimumWidth(
                    iconSize = regularIconSize,
                    iconValueSpacing = iconValueSpacing,
                    groupSpacing = regularGroupSpacing,
                    humidityValueColumnWidth = humidityValueColumnWidth,
                    feelsLikeValueColumnWidth = feelsLikeValueColumnWidth,
                    weatherValueColumnWidth = weatherValueColumnWidth,
                )
            val useCompactGroupSpacing = availableContentWidth < compactIconMinimumContentWidth
            val groupSpacing =
                if (useCompactGroupSpacing) {
                    compactGroupSpacing
                } else {
                    regularGroupSpacing
                }
            val nonIconContentWidth =
                iconValueSpacing * 3 +
                    groupSpacing * 4 +
                    HeartGuardBorderWidth.Divider * 2 +
                    humidityValueColumnWidth +
                    feelsLikeValueColumnWidth +
                    weatherValueColumnWidth
            val iconSize =
                if (remainingWidthDeficit > 0.dp) {
                    ((availableContentWidth - nonIconContentWidth) / 3)
                        .coerceAtMost(regularIconSize)
                        .coerceAtLeast(MIN_COMPACT_HOME_METRIC_ICON_SIZE)
                } else {
                    regularIconSize
                }
            val scrollState = rememberScrollState()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = HeartGuardSpacing.Section,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(groupSpacing),
            ) {
                HomeWeatherMetric(
                    iconPainter = painterResource(R.drawable.home_humidity),
                    metricLabel = humidityLabel,
                    metricValue = humidity,
                    modifier =
                        Modifier
                            .width(
                                metricWrapperWidth(
                                    iconSize = iconSize,
                                    iconValueSpacing = iconValueSpacing,
                                    valueColumnWidth = humidityValueColumnWidth,
                                ),
                            ).testTag("home-weather-metric-humidity"),
                    iconTestTag = "home-weather-icon-humidity",
                    iconSize = iconSize,
                    iconValueSpacing = iconValueSpacing,
                    valueColumnWidth = humidityValueColumnWidth,
                    valueColor = HOME_METRIC_FIGMA_VALUE_COLOR,
                    labelColor = HOME_METRIC_FIGMA_LABEL_COLOR,
                )

                HomeWeatherMetricDivider(testTag = "home-weather-divider-first")

                HomeWeatherMetric(
                    iconPainter = painterResource(R.drawable.home_feels_like),
                    metricLabel = feelsLikeTemperatureLabel,
                    metricValue = feelsLikeTemperature,
                    modifier =
                        Modifier
                            .width(
                                metricWrapperWidth(
                                    iconSize = iconSize,
                                    iconValueSpacing = iconValueSpacing,
                                    valueColumnWidth = feelsLikeValueColumnWidth,
                                ),
                            ).testTag("home-weather-metric-feels-like"),
                    iconTestTag = "home-weather-icon-feels-like",
                    iconSize = iconSize,
                    iconValueSpacing = iconValueSpacing,
                    valueColumnWidth = feelsLikeValueColumnWidth,
                    valueColor = HOME_METRIC_FIGMA_VALUE_COLOR,
                    labelColor = HOME_METRIC_FIGMA_LABEL_COLOR,
                )

                HomeWeatherMetricDivider(testTag = "home-weather-divider-second")

                HomeWeatherMetric(
                    iconPainter = painterResource(R.drawable.home_weather),
                    metricLabel = weatherLabel,
                    metricValue = weatherValue,
                    modifier =
                        Modifier
                            .width(
                                metricWrapperWidth(
                                    iconSize = iconSize,
                                    iconValueSpacing = iconValueSpacing,
                                    valueColumnWidth = weatherValueColumnWidth,
                                ),
                            ).testTag("home-weather-metric-weather"),
                    iconTestTag = "home-weather-icon-weather",
                    iconSize = iconSize,
                    iconValueSpacing = iconValueSpacing,
                    valueColumnWidth = weatherValueColumnWidth,
                    valueColor = HOME_METRIC_FIGMA_VALUE_COLOR,
                    labelColor = HOME_METRIC_FIGMA_LABEL_COLOR,
                )
            }
        }
    }
}

@Composable
private fun HomeWeatherMetric(
    iconPainter: Painter,
    metricLabel: String,
    metricValue: String,
    modifier: Modifier = Modifier,
    iconTestTag: String? = null,
    iconSize: Dp,
    iconValueSpacing: Dp,
    valueColumnWidth: Dp? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val iconInnerPadding =
        HeartGuardSpacing.Compact *
            (iconSize.value / HeartGuardIconSize.HomeMetric.value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(iconValueSpacing),
    ) {
        Surface(
            modifier =
                Modifier
                    .size(iconSize)
                    .then(iconTestTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier),
            shape = RoundedCornerShape(HeartGuardRadius.Pill),
            color = MaterialTheme.extraColors.homeMetricContainer,
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.padding(iconInnerPadding),
            )
        }

        val valueParts = metricValueParts(metricValue)
        val metricLabelStyle =
            MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.sp,
            )
        val metricValueStyle =
            MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp,
            )
        Column(modifier = valueColumnWidth?.let { width -> Modifier.width(width) } ?: Modifier) {
            Text(
                text = metricLabel,
                color = labelColor,
                style = metricLabelStyle,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueParts.number,
                    modifier = if (valueParts.unit.isNotEmpty()) Modifier.weight(1f, fill = false) else Modifier,
                    color = valueColor,
                    style = metricValueStyle,
                    maxLines = 1,
                    softWrap = false,
                )
                if (valueParts.unit.isNotEmpty()) {
                    Text(
                        text = valueParts.unit,
                        color = valueColor,
                        style = metricValueStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

private fun metricValueParts(value: String): HomeMetricValueParts {
    val unit =
        when {
            value.endsWith(HUMIDITY_UNIT_SUFFIX) -> HUMIDITY_UNIT_SUFFIX
            value.endsWith(TEMPERATURE_UNIT_SUFFIX) -> TEMPERATURE_UNIT_SUFFIX
            else -> ""
        }
    return HomeMetricValueParts(
        number = value.removeSuffix(unit),
        unit = unit,
    )
}

private data class HomeMetricValueParts(
    val number: String,
    val unit: String,
)

private const val HUMIDITY_UNIT_SUFFIX = "%"
private const val TEMPERATURE_UNIT_SUFFIX = "°C"
private val HOME_METRIC_FIGMA_LABEL_COLOR = Color(0xFF6B7280)
private val HOME_METRIC_FIGMA_VALUE_COLOR = Color(0xFF30333D)
private val HOME_METRIC_FIGMA_DIVIDER_COLOR = Color(0xFFE8EBF5)
private val HOME_METRIC_FIGMA_SHADOW_ELEVATION = 4.dp
private val HomeHumidityValueColumnMinWidth = 36.dp
private val HomeFeelsLikeValueColumnMinWidth = 46.dp
private val HomeWeatherValueColumnMinWidth = 26.dp
private val MIN_COMPACT_HOME_METRIC_ICON_SIZE = 20.dp

private fun metricRowMinimumWidth(
    iconSize: Dp,
    iconValueSpacing: Dp,
    groupSpacing: Dp,
    humidityValueColumnWidth: Dp,
    feelsLikeValueColumnWidth: Dp,
    weatherValueColumnWidth: Dp,
): Dp =
    iconSize * 3 +
        iconValueSpacing * 3 +
        groupSpacing * 4 +
        HeartGuardBorderWidth.Divider * 2 +
        humidityValueColumnWidth +
        feelsLikeValueColumnWidth +
        weatherValueColumnWidth

private fun metricWrapperWidth(
    iconSize: Dp,
    iconValueSpacing: Dp,
    valueColumnWidth: Dp,
): Dp =
    iconSize +
        iconValueSpacing +
        valueColumnWidth

@Composable
private fun HomeWeatherMetricDivider(testTag: String) {
    Surface(
        modifier =
            Modifier
                .width(HeartGuardBorderWidth.Divider)
                .height(HeartGuardComponentSize.HomeMetricDividerHeight)
                .testTag(testTag),
        color = HOME_METRIC_FIGMA_DIVIDER_COLOR,
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
