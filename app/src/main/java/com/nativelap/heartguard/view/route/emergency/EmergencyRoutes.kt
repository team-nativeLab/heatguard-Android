package com.nativelap.heartguard.view.route.emergency

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen

/** 긴급 화면의 고정 현장 관리자 정보와 호출 취소 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    onCallClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = stringResource(R.string.emergency_contact_phone),
        onCallClick = onCallClick,
        onCancelClick = onCancelClick,
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
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = stringResource(R.string.emergency_contact_phone),
        onCancelClick = onCancelClick,
        onEndClick = onEndClick,
    )
}
