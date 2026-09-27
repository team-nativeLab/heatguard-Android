package com.nativelap.heartguard.view.route.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel

/** 홈에 팀 현장페이지 API 결과와 사용자 이벤트를 HomeScreen에 전달하는 Route이다.
 * 서버 응답을 아직 받지 못했거나([HomeUiState.Loading]) 실패했을 때([HomeUiState.Error])는
 * 기존 화면과 동일한 고정 표시값을 유지해, 백엔드가 아직 없는 개발 단계에서도 화면이
 * 깨지지 않게 한다. 성공 응답을 받으면 실제 값으로 대체한다.
 * "관리자 전화"는 Emergency 화면으로 이동하지 않고 이 Route에서 바로 다이얼러를 여는 반면,
 * "긴급 전화"([onEmergencyClick])는 긴급호출 흐름(Emergency 화면)으로 이동한다 — 두 버튼의
 * 목적이 다르므로(즉시 통화 vs 긴급호출 절차 시작) 의도적으로 다른 방식으로 동작한다. */
@Composable
internal fun HeartGuardHomeRoute(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onEmergencyClick: () -> Unit,
    onFieldPhotoClick: () -> Unit,
    onRecordHistoryClick: () -> Unit,
    onRecordClick: () -> Unit,
) {
    // TODO: androidx.lifecycle:lifecycle-runtime-compose 도입이 확정되면 collectAsStateWithLifecycle로 교체한다.
    val uiState by homeViewModel.uiState.collectAsState()
    val overview = (uiState as? HomeUiState.Success)?.overview

    // 기상청 폭염특보 4단계(관심/주의/경고/위험)에 맞춘 라벨이다. 서버 값이 아직 없으면(로딩·실패)
    // 기존 화면과 동일하게 "주의" 단계를 기본값으로 보여준다.
    val riskLabel = when (overview?.heatLevel) {
        0 -> stringResource(R.string.home_heat_level_interest)
        2 -> stringResource(R.string.home_heat_level_warning)
        3 -> stringResource(R.string.home_heat_level_danger)
        else -> stringResource(R.string.home_heat_caution)
    }

    val dialPhoneNumber = rememberPhoneDialLauncher()
    // 서버가 실제 관리자 전화번호(TeamSiteOverview.managerPhoneNumber)를 내려주면 그 값을 쓰고,
    // 아직 응답을 못 받았으면 기존 화면과 동일한 고정 번호로 대체한다.
    val managerPhoneNumber = overview?.managerPhoneNumber
        ?: stringResource(R.string.home_manager_contact_phone)

    HomeScreen(
        currentTemperature = overview?.let { "${it.currentTemperature}°C" } ?: "47.5°C",
        feelsLikeTemperature = overview?.let { "${it.apparentTemperature}°C" } ?: "40.5°C",
        humidity = overview?.let {
            stringResource(
                R.string.home_humidity_value_format,
                it.humidity.toDisplayNumber(),
            )
        } ?: "55%",
        temperatureDelta = "+3.2°C",
        riskLabel = riskLabel,
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

// 서버 수치를 화면에 표시할 문자열로 바꾼다. 55.0처럼 소수부가 0이면 "55"로, 55.5는 그대로 "55.5"로 보여준다.
private fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}
