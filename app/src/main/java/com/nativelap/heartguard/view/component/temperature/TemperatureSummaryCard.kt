package com.nativelap.heartguard.view.component.temperature

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
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
    thermometerPainter: Painter? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HeartGuardComponentSize.TemperatureSummaryHeight)
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            if (thermometerPainter != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                ) {
                    Image(
                        painter = thermometerPainter,
                        contentDescription = null,
                        modifier = Modifier.size(HeartGuardIconSize.Information),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
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
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = HeartGuardFontSize.TemperatureSummary,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(
                                R.string.home_weather_summary_format,
                                humidityLabel,
                                humidity,
                                feelsLikeLabel,
                                feelsLikeTemperature,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
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
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
                        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
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
            thermometerPainter = painterResource(R.drawable.record_temperature),
        )
    }
}
