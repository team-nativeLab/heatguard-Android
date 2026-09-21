package com.nativelap.heartguard.view.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.home.HomeAdditionalRecordCard
import com.nativelap.heartguard.view.component.home.HomeCheckTimeline
import com.nativelap.heartguard.view.component.home.HomeContactCard
import com.nativelap.heartguard.view.component.home.WeatherStatusCard
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton

/** Figma 홈 리디자인의 날씨, 점검, 연락처, 추가 기록 영역을 하나의 화면으로 조립한다. */
@Composable
fun HomeScreen(
    currentTemperature: String,
    feelsLikeTemperature: String,
    humidity: String,
    temperatureDelta: String,
    riskLabel: String,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onManagerCallClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onRecordClick: () -> Unit,
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
                top = HeartGuardSpacing.PageContentTop,
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
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                    weatherPainter = painterResource(R.drawable.heartguard_home_weather),
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
                Text(
                    text = stringResource(R.string.home_data_records),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            item {
                HomeCheckTimeline(
                    title = stringResource(R.string.home_today_check_title),
                    nextCheckDescription = stringResource(R.string.home_next_check_description),
                    items = homeCheckTimelineItems(),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                )
            }

            item {
                HomeContactCard(
                    managerTitle = stringResource(R.string.home_manager_call),
                    managerDescription = stringResource(R.string.home_manager_call_description),
                    emergencyTitle = stringResource(R.string.home_emergency_call),
                    emergencyDescription = stringResource(R.string.home_emergency_call_description),
                    onManagerClick = onManagerCallClick,
                    onEmergencyClick = onEmergencyClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.home_additional_records),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_field_photo),
                    description = stringResource(R.string.home_field_photo_description),
                    iconPainter = painterResource(R.drawable.home_photo),
                    onClick = onFieldPhotoClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_record_history),
                    description = stringResource(R.string.home_record_history_description),
                    iconPainter = painterResource(R.drawable.home_history),
                    onClick = onRecordHistoryClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                )
            }

            item {
                RecordSaveButton(
                    title = stringResource(R.string.home_record_action),
                    onClick = onRecordClick,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
                )
            }
        }
    }
}

/** 홈 Preview와 로컬 샘플 화면에서 Figma의 08시~22시 점검 상태를 재현한다. */
private fun homeCheckTimelineItems(): List<CheckTimelineItem> {
    return listOf(
        CheckTimelineItem("08시", isCompleted = true),
        CheckTimelineItem("10시", isCompleted = true),
        CheckTimelineItem("12시", isCompleted = false),
        CheckTimelineItem("14시", isCompleted = true),
        CheckTimelineItem("16시", isCompleted = false),
        CheckTimelineItem("18시", isCompleted = true),
        CheckTimelineItem("20시", isCompleted = true, isCurrent = true),
        CheckTimelineItem("22시", isCompleted = false),
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun HomeScreenPreview() {
    HeartGuardTheme {
        HomeScreen(
            currentTemperature = "47.5°C",
            feelsLikeTemperature = "40.5°C",
            humidity = "55%",
            temperatureDelta = "+3.2°C",
            riskLabel = stringResource(R.string.home_heat_caution),
            onMenuClick = {},
            onNotificationClick = {},
            onManagerCallClick = {},
            onEmergencyClick = {},
            onFieldPhotoClick = {},
            onRecordHistoryClick = {},
            onRecordClick = {},
        )
    }
}
