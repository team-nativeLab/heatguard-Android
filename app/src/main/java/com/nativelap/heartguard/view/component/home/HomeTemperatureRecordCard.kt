package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 리디자인 홈에서 자동·수동 온도 기록 상태와 세부 지표를 함께 보여준다. */
@Composable
fun HomeTemperatureRecordCard(
    title: String,
    isAutomaticRecordEnabled: Boolean,
    onAutomaticRecordChange: (Boolean) -> Unit,
    metrics: List<WeatherMetricValue>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = isAutomaticRecordEnabled,
                    onCheckedChange = onAutomaticRecordChange,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                metrics.take(3).forEach { metric ->
                    WeatherMetric(
                        metricLabel = metric.label,
                        metricValue = metric.value,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

data class WeatherMetricValue(
    val label: String,
    val value: String,
)

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun HomeTemperatureRecordCardPreview() {
    HeartGuardTheme {
        HomeTemperatureRecordCard(
            title = "오늘의 온도 기록",
            isAutomaticRecordEnabled = true,
            onAutomaticRecordChange = {},
            metrics = listOf(
                WeatherMetricValue("현재 온도", "37℃"),
                WeatherMetricValue("체감온도", "40℃"),
                WeatherMetricValue("습도", "65%"),
            ),
        )
    }
}
