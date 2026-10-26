package com.nativelap.heartguard.view.route.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel
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
) {
    val context = LocalContext.current
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val checkSchedule by homeViewModel.checkSchedule.collectAsStateWithLifecycle()
    val menuDrawerProfile by menuDrawerViewModel.profile.collectAsStateWithLifecycle()
    val loadFailureMessage = stringResource(R.string.home_load_failure)

    // 기록 저장 뒤나 앱을 다시 열었을 때 날씨·체크 완료가 갱신되도록 홈이 다시 보일 때마다 새로 조회한다.
    LifecycleResumeEffect(homeViewModel) {
        homeViewModel.refresh()
        onPauseOrDispose {}
    }

    // 홈 서버 조회 실패를 Toast로 알린다.
    LaunchedEffect(homeUiState) {
        if (homeUiState is HomeUiState.Error) {
            Toast.makeText(context, loadFailureMessage, Toast.LENGTH_SHORT).show()
        }
    }
    var isMenuDrawerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    // 드로어가 열려 있을 때만 시스템 뒤로가기를 가로채 드로어를 닫는다. 닫혀 있으면 NavHost의 onBack이 처리한다.
    BackHandler(enabled = isMenuDrawerOpen) {
        isMenuDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            currentTemperature = homeTemperatureText(siteStatus.temperature),
            feelsLikeTemperature = temperatureValueText(siteStatus.apparentTemperature),
            humidity = humidityValueText(siteStatus.humidity),
            temperatureDelta = temperatureValueText(siteStatus.temperatureDelta),
            isTemperatureIncreasing = siteStatus.isTemperatureIncreasing,
            weatherValue = skyStatusText(siteStatus.skyStatus),
            weatherImageRes = skyStatusImageRes(siteStatus.skyStatus),
            riskLabel = heatLevelLabelText(siteStatus.heatLevel),
            nextCheckDescription = nextCheckDescriptionText(checkSchedule),
            checkTimelineItems = checkTimelineItems(checkSchedule),
            onMenuClick = {
                isMenuDrawerOpen = true
                menuDrawerViewModel.loadProfile()
            },
            onNotificationClick = onNotificationsClick,
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

                        MenuDrawerEvent.LogoutClicked -> {
                            isMenuDrawerOpen = false
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

// 눈 전용 이미지가 없어 눈·미입력은 기본 홈 날씨 이미지를 쓴다.
@DrawableRes
private fun skyStatusImageRes(skyStatus: SkyStatus?): Int =
    when (skyStatus) {
        SkyStatus.CLEAR -> R.drawable.weather_sunny

        SkyStatus.CLOUDY -> R.drawable.weather_cloud

        SkyStatus.RAIN -> R.drawable.weather_rain

        SkyStatus.PARTLY_CLOUDY,
        SkyStatus.SNOW,
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
