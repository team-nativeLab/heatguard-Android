package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.home.WeatherMetricValue
import com.nativelap.heartguard.view.screen.home.HomeScreen

/** 홈의 직접 입력 Switch 상태를 보유하고 Figma 홈 Screen에 로컬 이벤트를 연결한다. */
@Composable
internal fun HeartGuardHomeRoute(
    onTemperatureRecordClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
) {
    var isManualInputEnabled by rememberSaveable {
        mutableStateOf(false)
    }

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
        isManualInputEnabled = isManualInputEnabled,
        onManualInputChange = { isManualInputEnabled = it },
        onMenuClick = {},
        onNotificationClick = {},
        onTemperatureRecordClick = onTemperatureRecordClick,
        onFieldPhotoClick = onFieldPhotoClick,
        onRecordHistoryClick = {},
    )
}
