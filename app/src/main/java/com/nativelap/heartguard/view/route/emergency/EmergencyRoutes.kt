package com.nativelap.heartguard.view.route.emergency

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen

/** 긴급 화면의 고정 현장 관리자 정보와 호출 취소 Navigation callback을 연결한다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    onCallClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val phoneNumber = stringResource(R.string.emergency_contact_phone)

    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = phoneNumber,
        onCallClick = onCallClick,
        onCancelClick = onCancelClick,
        onContactClick = { dialPhoneNumber(phoneNumber) },
    )
}

/** 호출 대기 Screen을 표시하고 취소·종료 목적지 callback을 전달한다. */
@Composable
internal fun HeartGuardCallingRoute(
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val phoneNumber = stringResource(R.string.emergency_contact_phone)

    CallingScreen(
        // TODO: 긴급호출 상태 폴링 API(GET .../emergency-calls/current) 연동 Scope에서
        // 응답 status가 ACKNOWLEDGED일 때 true가 되도록 ViewModel에서 내려주는 실제 값으로 교체한다.
        // 지금은 폴링 API가 없어 항상 대기(false) 상태로 고정한다.
        isConnected = false,
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = phoneNumber,
        onCancelClick = onCancelClick,
        onEndClick = onEndClick,
        onContactClick = { dialPhoneNumber(phoneNumber) },
    )
}
