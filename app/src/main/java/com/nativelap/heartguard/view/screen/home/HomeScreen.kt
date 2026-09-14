package com.nativelap.heartguard.view.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.home.HomeAdditionalRecordCard
import com.nativelap.heartguard.view.component.home.HomeTemperatureRecordCard
import com.nativelap.heartguard.view.component.home.WeatherMetricValue
import com.nativelap.heartguard.view.component.home.WeatherStatusCard

/** Figma 현장앱 홈의 날씨·데이터 기록·추가 기록 영역을 조합한다. */
@Composable
fun HomeScreen(
    currentTemperature: String,
    feelsLikeTemperature: String,
    humidity: String,
    temperatureDelta: String,
    riskLabel: String,
    temperatureMetrics: List<WeatherMetricValue>,
    isManualInputEnabled: Boolean,
    onManualInputChange: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onTemperatureRecordClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = HeartGuardSpacing.ScreenHorizontal,
                top = HeartGuardSpacing.PageContentTop,
                end = HeartGuardSpacing.ScreenHorizontal,
                bottom = HeartGuardSpacing.LargeSection,
            ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            item {
                HeartGuardHeader(
                    title = stringResource(R.string.brand_name),
                    menuPainter = painterResource(R.drawable.menu_hamburger),
                    notificationPainter = painterResource(R.drawable.notification_bell),
                    onMenuClick = onMenuClick,
                    onNotificationClick = onNotificationClick,
                )
            }

            item {
                WeatherStatusCard(
                    weatherPainter = painterResource(R.drawable.weather_sunny),
                    weatherContentDescription = stringResource(R.string.weather_sunny_description),
                    statusTitle = stringResource(R.string.home_weather_status),
                    currentTemperature = currentTemperature,
                    feelsLikeTemperature = feelsLikeTemperature,
                    feelsLikeTemperatureLabel = stringResource(R.string.home_feels_like),
                    humidity = humidity,
                    humidityLabel = stringResource(R.string.home_humidity),
                    weatherValue = stringResource(R.string.weather_sunny_description),
                    weatherLabel = stringResource(R.string.home_weather_label),
                    temperatureDeltaLabel = stringResource(R.string.home_temperature_change),
                    temperatureDelta = temperatureDelta,
                    riskLabel = riskLabel,
                    isTemperatureIncreasing = true,
                )
            }

            item {
                HomeTemperatureRecordCard(
                    title = stringResource(R.string.home_temperature_records),
                    recordHint = stringResource(R.string.home_temperature_record_hint),
                    manualInputTitle = stringResource(R.string.home_manual_temperature_input),
                    isManualInputEnabled = isManualInputEnabled,
                    onManualInputChange = onManualInputChange,
                    metrics = temperatureMetrics,
                    onClick = onTemperatureRecordClick,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_additional_records),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_field_photo),
                    description = stringResource(R.string.home_field_photo_description),
                    icon = Icons.Outlined.CameraAlt,
                    onClick = onFieldPhotoClick,
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_record_history),
                    description = stringResource(R.string.home_record_history_description),
                    // Figma 02_홈_리디자인: "기록 내역" 카드는 책 아이콘이 아닌 막대그래프 아이콘을 사용한다.
                    icon = Icons.Outlined.BarChart,
                    onClick = onRecordHistoryClick,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun HomeScreenPreview() {
    HeartGuardTheme {
        HomeScreen(
            currentTemperature = "47.5°C",
            feelsLikeTemperature = "40.5°C",
            humidity = "55%",
            temperatureDelta = "+3.2°C",
            riskLabel = stringResource(R.string.home_heat_caution),
            temperatureMetrics = listOf(
                WeatherMetricValue(
                    label = stringResource(R.string.home_temperature_field),
                    value = "47.5",
                ),
                WeatherMetricValue(
                    label = stringResource(R.string.home_humidity_field),
                    value = "55",
                ),
                WeatherMetricValue(
                    label = stringResource(R.string.home_feels_like_field),
                    value = stringResource(R.string.home_calculated),
                ),
            ),
            isManualInputEnabled = false,
            onManualInputChange = {},
            onMenuClick = {},
            onNotificationClick = {},
            onTemperatureRecordClick = {},
            onFieldPhotoClick = {},
            onRecordHistoryClick = {},
        )
    }
}
