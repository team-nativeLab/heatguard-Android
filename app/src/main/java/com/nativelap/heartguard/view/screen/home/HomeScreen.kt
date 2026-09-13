package com.nativelap.heartguard.view.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardHeader
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.home.HomeActionCard
import com.nativelap.heartguard.view.component.home.HomeAdditionalRecordCard
import com.nativelap.heartguard.view.component.home.HomeCheckTimeline
import com.nativelap.heartguard.view.component.home.HomeInfoCard
import com.nativelap.heartguard.view.component.home.HomeTemperatureRecordCard
import com.nativelap.heartguard.view.component.home.WeatherMetricValue
import com.nativelap.heartguard.view.component.home.WeatherStatusCard

/** 홈 화면의 날씨·기록·현장 정보를 세로 스크롤 구조로 조합한다. */
@Composable
fun HomeScreen(
    currentTemperature: String,
    feelsLikeTemperature: String,
    humidity: String,
    temperatureDelta: String,
    riskLabel: String,
    siteName: String,
    teamName: String,
    timelineItems: List<CheckTimelineItem>,
    temperatureMetrics: List<WeatherMetricValue>,
    workPhotoDescription: String,
    restPhotoDescription: String,
    isAutomaticRecordEnabled: Boolean,
    onAutomaticRecordChange: (Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onManagerClick: () -> Unit,
    onSiteClick: () -> Unit,
    onTeamClick: () -> Unit,
    onTemperatureRecordClick: () -> Unit = {},
    onWorkPhotoClick: () -> Unit,
    onRestPhotoClick: () -> Unit,
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
                top = HeartGuardSpacing.Compact,
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
                    currentTemperatureLabel = stringResource(R.string.home_current_temperature),
                    feelsLikeTemperature = feelsLikeTemperature,
                    feelsLikeTemperatureLabel = stringResource(R.string.home_feels_like),
                    humidity = humidity,
                    humidityLabel = stringResource(R.string.home_humidity),
                    temperatureDeltaLabel = stringResource(R.string.home_temperature_change),
                    temperatureDelta = temperatureDelta,
                    riskLabel = riskLabel,
                    isTemperatureIncreasing = true,
                )
            }

            item {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = if (
                            maxWidth < HeartGuardComponentSize.CompactLayoutBreakpoint
                        ) {
                            1
                        } else {
                            2
                        },
                        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                    ) {
                        HomeActionCard(
                            title = stringResource(R.string.home_emergency_action),
                            icon = Icons.Outlined.ReportProblem,
                            onClick = onEmergencyClick,
                            modifier = Modifier.weight(1f),
                            isDestructive = true,
                        )
                        HomeActionCard(
                            title = stringResource(R.string.home_manager_action),
                            icon = Icons.Outlined.Settings,
                            onClick = onManagerClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                HomeTemperatureRecordCard(
                    title = stringResource(R.string.home_temperature_records),
                    isAutomaticRecordEnabled = isAutomaticRecordEnabled,
                    onAutomaticRecordChange = onAutomaticRecordChange,
                    metrics = temperatureMetrics,
                    onClick = onTemperatureRecordClick,
                )
            }

            item {
                HomeCheckTimeline(
                    title = stringResource(R.string.home_today_checks),
                    items = timelineItems,
                )
            }

            item {
                HomeInfoCard(
                    title = stringResource(R.string.home_site_information),
                    value = siteName,
                    icon = Icons.Outlined.LocationCity,
                    onClick = onSiteClick,
                )
            }

            item {
                HomeInfoCard(
                    title = stringResource(R.string.home_team_information),
                    value = teamName,
                    icon = Icons.Outlined.Settings,
                    onClick = onTeamClick,
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_work_photo_record),
                    description = workPhotoDescription,
                    icon = Icons.Outlined.CameraAlt,
                    onClick = onWorkPhotoClick,
                )
            }

            item {
                HomeAdditionalRecordCard(
                    title = stringResource(R.string.home_rest_photo_record),
                    description = restPhotoDescription,
                    icon = Icons.Outlined.CameraAlt,
                    onClick = onRestPhotoClick,
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
            isAutomaticRecordEnabled = true,
            onAutomaticRecordChange = {},
            onMenuClick = {},
            onNotificationClick = {},
            onEmergencyClick = {},
            onManagerClick = {},
            onSiteClick = {},
            onTeamClick = {},
            onWorkPhotoClick = {},
            onRestPhotoClick = {},
        )
    }
}
