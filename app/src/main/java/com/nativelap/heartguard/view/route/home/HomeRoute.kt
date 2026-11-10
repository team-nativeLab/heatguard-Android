package com.nativelap.heartguard.view.route.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.domain.site.model.SkyStatus
import com.nativelap.heartguard.view.component.emptyValueText
import com.nativelap.heartguard.view.component.heatLevelLabelText
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.humidityValueText
import com.nativelap.heartguard.view.component.menu.MenuDrawerContent
import com.nativelap.heartguard.view.component.menu.MenuDrawerOverlay
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.view.route.menu.CollectMenuDrawerLogoutBlocked
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.viewmodel.home.HomeRiskBadgeType
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.home.LocationWeatherStatus
import com.nativelap.heartguard.viewmodel.home.toHomeRiskBadgeType
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationUnreadViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** 홈에 팀 현장페이지 API 결과와 사용자 이벤트를 HomeScreen에 전달하는 Route이다.
 * 서버 응답을 아직 받지 못했거나 실패했으면 모든 서버 값을 "--"로 보여준다(고정 표시값을 쓰지 않는다).
 * 체크 완료 여부는 서버 필드가 없어 오늘 기록의 측정 시각으로 판정한다.
 * "관리자 전화"([onManagerCallClick])는 Navigation 3의 본사 연락처 시트로 이동하고,
 * "긴급 전화"([onEmergencyClick])는 현장관리자 긴급호출 흐름(Emergency 화면)으로 이동한다.
 * 메뉴 드로어는 홈 헤더에서만 열리는 오버레이라 이 Route가 열림 상태를 직접 소유한다.
 * 메뉴 드로어만 이 화면의 로컬 상태로 관리하고 전화·기록 오버레이는 Navigation 3 목적지로 처리한다. */
@Composable
internal fun HeartGuardHomeRoute(
    homeViewModel: HomeViewModel,
    onManagerCallClick: () -> Unit,
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onProfileEditClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onChecklistClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSiteSelectionClick: () -> Unit,
    menuDrawerViewModel: MenuDrawerViewModel = hiltViewModel(),
    unreadViewModel: TeamNotificationUnreadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var locationPermissionRequestCount by rememberSaveable { mutableStateOf(0) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                homeViewModel.loadCurrentLocationWeather()
            } else {
                homeViewModel.markLocationPermissionDenied()
            }
        }
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val locationWeatherStatus by homeViewModel.locationWeatherStatus.collectAsStateWithLifecycle()
    val checkSchedule by homeViewModel.checkSchedule.collectAsStateWithLifecycle()
    val menuDrawerProfile by menuDrawerViewModel.profile.collectAsStateWithLifecycle()
    val isMenuDrawerLoggingOut by menuDrawerViewModel.isLoggingOut.collectAsStateWithLifecycle()
    CollectMenuDrawerLogoutBlocked(menuDrawerViewModel)
    val unreadUiState by unreadViewModel.uiState.collectAsStateWithLifecycle()
    val loadFailureMessage = stringResource(R.string.home_load_failure)

    LaunchedEffect(homeViewModel, locationPermissionLauncher) {
        if (!hasRequestedLocationPermission) {
            hasRequestedLocationPermission = true
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                locationPermissionRequestCount += 1
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    // 기록 저장 뒤나 앱을 다시 열었을 때 날씨·체크 완료가 갱신되도록 홈이 다시 보일 때마다 새로 조회한다.
    LifecycleResumeEffect(homeViewModel) {
        homeViewModel.refresh()
        unreadViewModel.refresh()
        val isLocationPermissionGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        if (isLocationPermissionGranted) {
            homeViewModel.loadCurrentLocationWeather()
        } else {
            homeViewModel.markLocationPermissionDenied()
        }
        onPauseOrDispose {}
    }

    // 홈 서버 조회 실패를 Toast로 알린다.
    LaunchedEffect(homeUiState) {
        if (homeUiState is HomeUiState.Error) {
            Toast.makeText(context, loadFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }
    val shouldShowLocationPermissionRationale =
        (context as? ComponentActivity)?.let { activity ->
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } == true
    val shouldOpenAppSettingsForLocationPermission =
        locationPermissionRequestCount >= LOCATION_PERMISSION_SETTINGS_THRESHOLD &&
            !shouldShowLocationPermissionRationale
    var isMenuDrawerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    // 드로어가 열려 있을 때만 시스템 뒤로가기를 가로채 드로어를 닫는다. 닫혀 있으면 NavHost의 onBack이 처리한다.
    BackHandler(enabled = isMenuDrawerOpen) {
        isMenuDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val riskBadgeType = siteStatus.toHomeRiskBadgeType()
        HomeScreen(
            currentTemperature = homeTemperatureText(siteStatus.temperature),
            feelsLikeTemperature = temperatureValueText(siteStatus.apparentTemperature),
            humidity = humidityValueText(siteStatus.humidity),
            temperatureDelta = temperatureValueText(siteStatus.temperatureDelta),
            isTemperatureIncreasing = siteStatus.isTemperatureIncreasing,
            isTemperatureDeltaZero = siteStatus.isTemperatureDeltaZero,
            weatherValue = skyStatusText(siteStatus.skyStatus),
            weatherImageRes = skyStatusImageRes(siteStatus.skyStatus),
            riskLabel =
                when (riskBadgeType) {
                    HomeRiskBadgeType.HEAT -> {
                        heatLevelLabelText(siteStatus.heatLevel)
                    }

                    HomeRiskBadgeType.COLD_WAVE_ADVISORY -> {
                        stringResource(R.string.home_cold_wave_advisory)
                    }

                    HomeRiskBadgeType.COLD_WAVE_WARNING -> {
                        stringResource(R.string.home_cold_wave_warning)
                    }
                },
            isColdWaveAlert = riskBadgeType != HomeRiskBadgeType.HEAT,
            weatherNotice =
                if (siteStatus.isLocationWeatherCached) {
                    siteStatus.weatherAgeMinutes?.let { elapsedMinutes ->
                        stringResource(R.string.home_weather_cache_age_format, elapsedMinutes)
                    }
                } else if (siteStatus.isLocationWeatherExpired) {
                    stringResource(R.string.home_weather_unavailable)
                } else {
                    when (locationWeatherStatus) {
                        LocationWeatherStatus.CheckingLocation,
                        LocationWeatherStatus.RequestingWeather,
                        LocationWeatherStatus.Idle,
                        LocationWeatherStatus.Available,
                        -> null

                        LocationWeatherStatus.LocationUnavailable ->
                            stringResource(R.string.home_weather_unavailable)

                        LocationWeatherStatus.WeatherRequestFailed ->
                            stringResource(R.string.home_weather_request_failed)

                        is LocationWeatherStatus.WeatherUnavailable ->
                            stringResource(R.string.home_weather_unavailable)

                        LocationWeatherStatus.PermissionDenied ->
                            stringResource(R.string.home_location_permission_denied)
                    }
                },
            coldWaveNotice =
                when {
                    siteStatus.coldWaveWarning?.availability ==
                        com.nativelap.heartguard.domain.site.model.ColdWaveAvailability.CACHED ||
                        siteStatus.coldWaveWarning?.isStale == true -> {
                        siteStatus.coldWaveAgeMinutes?.let { elapsedMinutes ->
                            stringResource(R.string.home_cold_wave_cache_age_format, elapsedMinutes)
                        }
                    }

                    siteStatus.isColdWaveWarningUnavailable ->
                        stringResource(R.string.home_cold_wave_unavailable)

                    else -> null
                },
            onWeatherRetry =
                when (locationWeatherStatus) {
                    LocationWeatherStatus.PermissionDenied -> {
                        {
                            if (!shouldOpenAppSettingsForLocationPermission) {
                                locationPermissionRequestCount += 1
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            } else {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            }
                        }
                    }

                    LocationWeatherStatus.LocationUnavailable,
                    LocationWeatherStatus.WeatherRequestFailed,
                    is LocationWeatherStatus.WeatherUnavailable,
                    -> {
                        ({ homeViewModel.loadCurrentLocationWeather() })
                    }

                    else -> {
                        null
                    }
                },
            weatherRetryLabel =
                if (locationWeatherStatus == LocationWeatherStatus.PermissionDenied &&
                    shouldOpenAppSettingsForLocationPermission
                ) {
                    stringResource(R.string.home_open_settings)
                } else {
                    stringResource(R.string.home_retry)
                },
            isRefreshingWeather =
                locationWeatherStatus == LocationWeatherStatus.CheckingLocation ||
                    locationWeatherStatus == LocationWeatherStatus.RequestingWeather,
            onRefreshWeather = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    homeViewModel.refreshCurrentLocationWeather()
                }
            },
            nextCheckDescription = nextCheckDescriptionText(checkSchedule),
            checkTimelineItems = checkTimelineItems(checkSchedule),
            onMenuClick = {
                isMenuDrawerOpen = true
                menuDrawerViewModel.loadProfile()
            },
            onNotificationClick = onNotificationsClick,
            hasUnreadNotifications = unreadUiState.hasUnread,
            onManagerCallClick = onManagerCallClick,
            onEmergencyClick = onEmergencyClick,
            onFieldPhotoClick = onFieldPhotoClick,
            onRecordHistoryClick = onRecordHistoryClick,
            onChecklistClick = onChecklistClick,
            onRecordClick = onRecordClick,
        )

        MenuDrawerOverlay(
            isVisible = isMenuDrawerOpen,
            onDismissRequest = {
                isMenuDrawerOpen = false
            },
        ) {
            MenuDrawerContent(
                profile = menuDrawerProfile,
                isLoggingOut = isMenuDrawerLoggingOut,
                onEvent = { event ->
                    when (event) {
                        MenuDrawerEvent.HomeClicked -> {
                            isMenuDrawerOpen = false
                            onHomeClick()
                        }

                        MenuDrawerEvent.EditProfileClicked -> {
                            isMenuDrawerOpen = false
                            onProfileEditClick()
                        }

                        MenuDrawerEvent.InquiryClicked -> {
                            isMenuDrawerOpen = false
                            onInquiryClick()
                        }

                        MenuDrawerEvent.SiteSelectionClicked -> {
                            isMenuDrawerOpen = false
                            onSiteSelectionClick()
                        }

                        // 로그아웃은 긴급 호출 종료가 확인될 때까지 드로어를 열어 둬 진행 표시와 차단 안내를 보이게 한다.
                        MenuDrawerEvent.LogoutClicked -> {
                            menuDrawerViewModel.logout()
                        }

                        MenuDrawerEvent.WithdrawClicked -> {
                            isMenuDrawerOpen = false
                            onWithdrawClick()
                        }
                    }
                },
            )
        }
    }
}

private const val LOCATION_PERMISSION_SETTINGS_THRESHOLD = 2

// 온도를 받지 못했으면 Figma 홈의 관측값 없음 표기("--.- °C")를 쓴다.
@Composable
@ReadOnlyComposable
private fun homeTemperatureText(temperature: String?): String =
    if (temperature == null) {
        stringResource(R.string.home_temperature_empty)
    } else {
        temperatureValueText(temperature)
    }

@Composable
@ReadOnlyComposable
private fun skyStatusText(skyStatus: SkyStatus?): String =
    when (skyStatus) {
        SkyStatus.CLEAR -> stringResource(R.string.home_sky_clear)

        SkyStatus.PARTLY_CLOUDY -> stringResource(R.string.home_sky_partly_cloudy)

        SkyStatus.CLOUDY -> stringResource(R.string.home_sky_cloudy)

        SkyStatus.RAIN -> stringResource(R.string.home_sky_rain)

        SkyStatus.SNOW -> stringResource(R.string.home_sky_snow)

        SkyStatus.UNKNOWN,
        null,
        -> emptyValueText()
    }

@DrawableRes
internal fun skyStatusImageRes(skyStatus: SkyStatus?): Int =
    when (skyStatus) {
        SkyStatus.CLEAR -> R.drawable.weather_sunny

        SkyStatus.CLOUDY -> R.drawable.weather_cloud

        SkyStatus.RAIN -> R.drawable.weather_rain

        SkyStatus.SNOW -> R.drawable.weather_snow

        SkyStatus.PARTLY_CLOUDY,
        SkyStatus.UNKNOWN,
        null,
        -> R.drawable.heartguard_home_weather
    }

private val nextCheckTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// "다음 체크까지 N분 · HH:mm 예정" 문구를 만든다. 체크 일정을 받지 못했으면 "--", 오늘 남은 체크가 없으면 종료 문구다.
@Composable
@ReadOnlyComposable
private fun nextCheckDescriptionText(checkSchedule: CheckSchedule?): String {
    if (checkSchedule == null || checkSchedule.checkTimes.isEmpty()) {
        return emptyValueText()
    }

    val nextCheckTime = checkSchedule.nextCheckTime
    val minutesUntilNextCheck = checkSchedule.minutesUntilNextCheck
    if (nextCheckTime == null || minutesUntilNextCheck == null) {
        return stringResource(R.string.home_check_finished)
    }

    val nextCheckTimeText = nextCheckTime.format(nextCheckTimeFormatter)
    val hoursUntilNextCheck = (minutesUntilNextCheck / MINUTES_PER_HOUR).toInt()
    val remainingMinutes = (minutesUntilNextCheck % MINUTES_PER_HOUR).toInt()

    return if (hoursUntilNextCheck > 0) {
        stringResource(
            R.string.home_next_check_hours_format,
            hoursUntilNextCheck,
            remainingMinutes,
            nextCheckTimeText,
        )
    } else {
        stringResource(
            R.string.home_next_check_minutes_format,
            remainingMinutes,
            nextCheckTimeText,
        )
    }
}

// 서버 체크 시각으로 타임라인 항목을 만든다. 완료 여부는 오늘 기록 측정 시각으로 판정한 값이며, 다음 체크를 강조한다.
@Composable
@ReadOnlyComposable
private fun checkTimelineItems(checkSchedule: CheckSchedule?): List<CheckTimelineItem> {
    if (checkSchedule == null) {
        return emptyList()
    }

    return checkSchedule.checkTimes.map { checkTime ->
        CheckTimelineItem(
            timeLabel = checkTimeLabelText(checkTime),
            isCompleted = checkTime in checkSchedule.completedCheckTimes,
            isCurrent = checkTime == checkSchedule.nextCheckTime,
        )
    }
}

// 정시는 "09시", 그 외에는 "09:30"처럼 보여준다.
@Composable
@ReadOnlyComposable
private fun checkTimeLabelText(checkTime: LocalTime): String =
    if (checkTime.minute == 0) {
        stringResource(R.string.home_check_time_hour_format, checkTime.hour)
    } else {
        checkTime.format(nextCheckTimeFormatter)
    }

private const val MINUTES_PER_HOUR = 60L
