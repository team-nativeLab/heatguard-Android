package com.nativelap.heartguard.view.route.emergency

import androidx.compose.runtime.Composable
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen

/** 긴급 화면의 고정 현장 관리자 정보와 호출 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    onCallClick: () -> Unit,
) {
    EmergencyScreen(
        contactName = "홍길동",
        phoneNumber = "010-1234-5678",
        onCallClick = onCallClick,
        onContactClick = {},
    )
}

/** 호출 대기 Screen을 표시하고 취소·종료 목적지 callback을 전달한다. */
@Composable
internal fun HeartGuardCallingRoute(
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    CallingScreen(
        isConnected = false,
        onCancelClick = onCancelClick,
        onEndClick = onEndClick,
    )
}
