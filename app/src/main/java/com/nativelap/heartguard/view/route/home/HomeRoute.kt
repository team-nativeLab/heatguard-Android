package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.home.WeatherMetricValue
import com.nativelap.heartguard.view.screen.home.HomeScreen

/** 홈 화면의 임시 표시 상태를 관리하고 Home Screen에 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardHomeRoute(
    onEmergencyClick: () -> Unit,
    onTemperatureRecordClick: () -> Unit,
    onWorkPhotoClick: () -> Unit,
    onRestPhotoClick: () -> Unit,
) {
    var isAutomaticRecordEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    HomeScreen(
        currentTemperature = "37℃",
        feelsLikeTemperature = "40℃",
        humidity = "65%",
        temperatureDelta = "+2℃",
        riskLabel = stringResource(R.string.home_heat_caution),
        siteName = "서울 강남구 건설 현장",
        teamName = "안전관리팀",
        timelineItems = listOf(
            CheckTimelineItem(
                timeLabel = "09:00",
                title = stringResource(R.string.home_check_morning),
                isCompleted = true,
            ),
            CheckTimelineItem(
                timeLabel = "13:00",
                title = stringResource(R.string.home_check_afternoon),
                isCompleted = false,
            ),
        ),
        temperatureMetrics = listOf(
            WeatherMetricValue(
                label = stringResource(R.string.home_current_temperature),
                value = "37℃",
            ),
            WeatherMetricValue(
                label = stringResource(R.string.home_feels_like),
                value = "40℃",
            ),
            WeatherMetricValue(
                label = stringResource(R.string.home_humidity),
                value = "65%",
            ),
        ),
        workPhotoDescription = "오늘 등록된 작업 사진 2장",
        restPhotoDescription = "오늘 등록된 휴식 사진 1장",
        isAutomaticRecordEnabled = isAutomaticRecordEnabled,
        onAutomaticRecordChange = { isAutomaticRecordEnabled = it },
        onMenuClick = {},
        onNotificationClick = {},
        onEmergencyClick = onEmergencyClick,
        onManagerClick = {},
        onSiteClick = {},
        onTeamClick = {},
        onTemperatureRecordClick = onTemperatureRecordClick,
        onWorkPhotoClick = onWorkPhotoClick,
        onRestPhotoClick = onRestPhotoClick,
    )
}
