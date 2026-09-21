package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.home.HomeScreen

/** 홈의 고정 샘플 데이터와 사용자 이벤트를 HomeScreen에 전달하는 Route이다. */
@Composable
internal fun HeartGuardHomeRoute(
    onManagerCallClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onRecordClick: () -> Unit,
) {
    HomeScreen(
        currentTemperature = "47.5°C",
        feelsLikeTemperature = "40.5°C",
        humidity = "55%",
        temperatureDelta = "+3.2°C",
        riskLabel = stringResource(R.string.home_heat_caution),
        onMenuClick = {},
        onNotificationClick = {},
        onManagerCallClick = onManagerCallClick,
        onEmergencyClick = onEmergencyClick,
        onFieldPhotoClick = onFieldPhotoClick,
        onRecordHistoryClick = onRecordHistoryClick,
        onRecordClick = onRecordClick,
    )
}
