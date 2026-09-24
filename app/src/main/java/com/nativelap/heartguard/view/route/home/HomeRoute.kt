package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.view.screen.home.HomeScreen

/** 홈의 고정 샘플 데이터와 사용자 이벤트를 HomeScreen에 전달하는 Route이다.
 * "관리자 전화"는 Emergency 화면으로 이동하지 않고 이 Route에서 바로 다이얼러를 여는 반면,
 * "긴급 전화"([onEmergencyClick])는 긴급호출 흐름(Emergency 화면)으로 이동한다 — 두 버튼의
 * 목적이 다르므로(즉시 통화 vs 긴급호출 절차 시작) 의도적으로 다른 방식으로 동작한다. */
@Composable
internal fun HeartGuardHomeRoute(
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onRecordClick: () -> Unit,
) {
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val managerPhoneNumber = stringResource(R.string.home_manager_contact_phone)

    HomeScreen(
        currentTemperature = "47.5°C",
        feelsLikeTemperature = "40.5°C",
        humidity = "55%",
        temperatureDelta = "+3.2°C",
        riskLabel = stringResource(R.string.home_heat_caution),
        // 메뉴·알림 아이콘 기능은 Figma/API 명세서 어디에도 정의되어 있지 않아 의도적으로 비워둔다.
        onMenuClick = {},
        onNotificationClick = {},
        onManagerCallClick = { dialPhoneNumber(managerPhoneNumber) },
        onEmergencyClick = onEmergencyClick,
        onFieldPhotoClick = onFieldPhotoClick,
        onRecordHistoryClick = onRecordHistoryClick,
        onRecordClick = onRecordClick,
    )
}
