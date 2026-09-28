package com.nativelap.heartguard.view.route.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.view.component.menu.MenuDrawerContent
import com.nativelap.heartguard.view.component.menu.MenuDrawerOverlay
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerViewModel
import kotlinx.coroutines.launch

/** 홈에 팀 현장페이지 API 결과와 사용자 이벤트를 HomeScreen에 전달하는 Route이다.
 * 서버 응답을 아직 받지 못했거나([HomeUiState.Loading]) 실패했을 때([HomeUiState.Error])는
 * 기존 화면과 동일한 고정 표시값을 유지해, 백엔드가 아직 없는 개발 단계에서도 화면이
 * 깨지지 않게 한다. 성공 응답을 받으면 실제 값으로 대체한다.
 * "관리자 전화"는 Emergency 화면으로 이동하지 않고 이 Route에서 바로 다이얼러를 여는 반면,
 * "긴급 전화"([onEmergencyClick])는 긴급호출 흐름(Emergency 화면)으로 이동한다 — 두 버튼의
 * 목적이 다르므로(즉시 통화 vs 긴급호출 절차 시작) 의도적으로 다른 방식으로 동작한다.
 * 메뉴 드로어는 홈 헤더에서만 열리는 오버레이라 이 Route가 열림 상태를 직접 소유한다.
 * android-navigation SKILL은 다이얼로그·바텀시트를 NavKey로 만들도록 하지만, 드로어는 사용자 결정에 따라
 * Navigation 3 목적지가 아닌 이 화면의 로컬 상태로 관리한다(드로어에서 시작하는 회원탈퇴만 NavHost로 이동). */
@Composable
internal fun HeartGuardHomeRoute(
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onRecordClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    menuDrawerViewModel: MenuDrawerViewModel = hiltViewModel(),
) {
    // TODO: androidx.lifecycle:lifecycle-runtime-compose 도입이 확정되면 collectAsStateWithLifecycle로 교체한다.
    val uiState by homeViewModel.uiState.collectAsState()
    val checklistUiState by homeViewModel.checklistUiState.collectAsState()
    val menuDrawerProfile by menuDrawerViewModel.profile.collectAsState()
    var isMenuDrawerOpen by rememberSaveable {
        mutableStateOf(false)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unsupportedFeatureMessage = stringResource(R.string.home_feature_unavailable)
    val showUnsupportedFeatureMessage: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = unsupportedFeatureMessage)
        }
        Unit
    }
    val overview = (uiState as? HomeUiState.Success)?.overview

    // 서버에서 폭염 단계 값을 받지 못한 경우 예시 단계 대신 값 없음 상태를 표시한다.
    val riskLabel = when (overview?.heatLevel) {
        0 -> stringResource(R.string.home_heat_level_interest)
        2 -> stringResource(R.string.home_heat_level_warning)
        3 -> stringResource(R.string.home_heat_level_danger)
        null -> stringResource(R.string.home_unavailable_value)
        else -> stringResource(R.string.home_heat_caution)
    }

    val dialPhoneNumber = rememberPhoneDialLauncher()
    val managerPhoneNumber = overview?.managerPhoneNumber?.takeIf(String::isNotBlank)

    // 드로어가 열려 있을 때만 시스템 뒤로가기를 가로채 드로어를 닫는다. 닫혀 있으면 NavHost의 onBack이 처리한다.
    BackHandler(enabled = isMenuDrawerOpen) {
        isMenuDrawerOpen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(
            currentTemperature = overview?.let { "${it.currentTemperature}°C" }
                ?: stringResource(R.string.home_unavailable_value),
            feelsLikeTemperature = overview?.let { "${it.apparentTemperature}°C" }
                ?: stringResource(R.string.home_unavailable_value),
            humidity = overview?.let {
                stringResource(
                    R.string.home_humidity_value_format,
                    it.humidity.toDisplayNumber(),
                )
            } ?: stringResource(R.string.home_unavailable_value),
            temperatureDelta = stringResource(R.string.home_unavailable_value),
            isTemperatureIncreasing = null,
            riskLabel = riskLabel,
            onMenuClick = {
                isMenuDrawerOpen = true
            },
            // 알림 기능은 API 명세에 없어 기능 안내를 보여준다.
            onNotificationClick = showUnsupportedFeatureMessage,
            onManagerCallClick = { managerPhoneNumber?.let(dialPhoneNumber) },
            onEmergencyClick = onEmergencyClick,
            onFieldPhotoClick = onFieldPhotoClick,
            onRecordHistoryClick = onRecordHistoryClick,
            onRecordClick = onRecordClick,
            isManagerCallEnabled = managerPhoneNumber != null,
            checklistState = checklistUiState,
            onChecklistItemChecked = homeViewModel::setChecklistItemChecked,
            onChecklistRetry = homeViewModel::loadTodayChecklist,
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
                        // 내 정보 수정·알림 설정·공지사항·고객센터는 Figma에 이동할 화면이 정의되어 있지 않아
                        // 화면이 추가될 때까지 의도적으로 아무 동작도 하지 않는다.
                        MenuDrawerEvent.EditProfileClicked,
                        MenuDrawerEvent.CustomerCenterClicked,
                        -> Unit

                        MenuDrawerEvent.NotificationSettingsClicked,
                        MenuDrawerEvent.NoticesClicked,
                        -> showUnsupportedFeatureMessage()

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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// 서버 수치를 화면에 표시할 문자열로 바꾼다. 55.0처럼 소수부가 0이면 "55"로, 55.5는 그대로 "55.5"로 보여준다.
private fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}
