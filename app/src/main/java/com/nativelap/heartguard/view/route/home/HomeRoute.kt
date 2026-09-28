package com.nativelap.heartguard.view.route.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.view.component.emptyValueText
import com.nativelap.heartguard.view.component.heatLevelLabelText
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.humidityValueText
import com.nativelap.heartguard.view.component.menu.MenuDrawerContent
import com.nativelap.heartguard.view.component.menu.MenuDrawerOverlay
import com.nativelap.heartguard.view.component.temperatureValueText
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** 홈에 팀 현장페이지 API 결과와 사용자 이벤트를 HomeScreen에 전달하는 Route이다.
 * 서버 응답을 아직 받지 못했거나 실패했으면 모든 서버 값을 "--"로 보여준다(고정 표시값을 쓰지 않는다).
 * 온도 변화량·날씨 상태는 overview API 필드에 없어 "--"로 둔다. 체크 완료 여부는 별도 체크리스트 API의
 * 응답과 홈 시간표 사이에 매핑 규칙이 없어 현재 연결하지 않는다.
 * "관리자 전화"는 Emergency 화면으로 이동하지 않고 이 Route에서 바로 다이얼러를 여는 반면,
 * "긴급 전화"([onEmergencyClick])는 긴급호출 흐름(Emergency 화면)으로 이동한다 — 두 버튼의
 * 목적이 다르므로(즉시 통화 vs 긴급호출 절차 시작) 의도적으로 다른 방식으로 동작한다.
 * 메뉴 드로어는 홈 헤더에서만 열리는 오버레이라 이 Route가 열림 상태를 직접 소유한다.
 * android-navigation SKILL은 다이얼로그·바텀시트를 NavKey로 만들도록 하지만, 드로어는 사용자 결정에 따라
 * Navigation 3 목적지가 아닌 이 화면의 로컬 상태로 관리한다(드로어에서 시작하는 회원탈퇴만 NavHost로 이동). */
@Composable
internal fun HeartGuardHomeRoute(
    homeViewModel: HomeViewModel,
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordClick: () -> Unit,
    onProfileEditClick: () -> Unit,
    onInquiryClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    menuDrawerViewModel: MenuDrawerViewModel = hiltViewModel(),
) {
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val checkSchedule by homeViewModel.checkSchedule.collectAsStateWithLifecycle()
    val menuDrawerProfile by menuDrawerViewModel.profile.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unavailableNotificationMessage = stringResource(R.string.home_notifications_unavailable)
    var isMenuDrawerOpen by rememberSaveable {
        mutableStateOf(false)
    }

    val dialPhoneNumber = rememberPhoneDialLauncher()
    val managerPhoneNumber = siteStatus.managerPhoneNumber

    // 드로어가 열려 있을 때만 시스템 뒤로가기를 가로채 드로어를 닫는다. 닫혀 있으면 NavHost의 onBack이 처리한다.
    BackHandler(enabled = isMenuDrawerOpen) {
        isMenuDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            currentTemperature = temperatureValueText(siteStatus.temperature),
            feelsLikeTemperature = temperatureValueText(siteStatus.apparentTemperature),
            humidity = humidityValueText(siteStatus.humidity),
            // TODO: 온도 변화량·날씨 상태는 API 명세에 필드가 없어 "--"로 표시한다. 필드가 추가되면 연결한다.
            temperatureDelta = emptyValueText(),
            isTemperatureIncreasing = null,
            weatherValue = emptyValueText(),
            riskLabel = heatLevelLabelText(siteStatus.heatLevel),
            nextCheckDescription = nextCheckDescriptionText(checkSchedule),
            checkTimelineItems = checkTimelineItems(checkSchedule),
            isManagerCallEnabled = managerPhoneNumber != null,
            onMenuClick = {
                isMenuDrawerOpen = true
            },
            onNotificationClick = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(unavailableNotificationMessage)
                }
            },
            onManagerCallClick = {
                managerPhoneNumber?.let(dialPhoneNumber)
            },
            onEmergencyClick = onEmergencyClick,
            onFieldPhotoClick = onFieldPhotoClick,
            onRecordHistoryClick = onRecordHistoryClick,
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
                        MenuDrawerEvent.EditProfileClicked -> {
                            isMenuDrawerOpen = false
                            onProfileEditClick()
                        }

                        MenuDrawerEvent.InquiryClicked -> {
                            isMenuDrawerOpen = false
                            onInquiryClick()
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = HeartGuardSpacing.HomeContentHorizontal),
        )
    }
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

// 서버 체크 시각으로 타임라인 항목을 만든다. 체크 완료 여부는 API에 없어 모두 미완료로 두고, 다음 체크만 강조한다.
@Composable
@ReadOnlyComposable
private fun checkTimelineItems(checkSchedule: CheckSchedule?): List<CheckTimelineItem> {
    if (checkSchedule == null) {
        return emptyList()
    }

    return checkSchedule.checkTimes.map { checkTime ->
        CheckTimelineItem(
            timeLabel = checkTimeLabelText(checkTime),
            isCompleted = false,
            isCurrent = checkTime == checkSchedule.nextCheckTime,
        )
    }
}

// 정시는 "09시", 그 외에는 "09:30"처럼 보여준다.
@Composable
@ReadOnlyComposable
private fun checkTimeLabelText(checkTime: LocalTime): String {
    return if (checkTime.minute == 0) {
        stringResource(R.string.home_check_time_hour_format, checkTime.hour)
    } else {
        checkTime.format(nextCheckTimeFormatter)
    }
}

private const val MINUTES_PER_HOUR = 60L
