package com.nativelap.heartguard.view.route.emergency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel

/** 긴급 화면의 고정 현장 관리자 정보와 호출 취소 Navigation callback을 연결한다.
 * 화면 진입 시 [emergencyViewModel]로 긴급호출을 등록하고 상태 폴링을 시작한다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    emergencyViewModel: EmergencyViewModel,
    onCallClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        emergencyViewModel.registerEmergencyCallIfNeeded()
    }

    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = stringResource(R.string.emergency_contact_phone),
        onCallClick = onCallClick,
        onCancelClick = onCancelClick,
        onContactClick = {},
    )
}

/** 호출 대기 Screen을 표시하고 취소·종료 목적지 callback을 전달한다.
 * [emergencyViewModel]이 폴링한 실제 상태(ACKNOWLEDGED)로 "연결됨" 화면을 보여준다. */
@Composable
internal fun HeartGuardCallingRoute(
    emergencyViewModel: EmergencyViewModel,
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    val uiState by emergencyViewModel.uiState.collectAsState()

    CallingScreen(
        isConnected = uiState.status.state == EmergencyCallState.ACKNOWLEDGED,
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = stringResource(R.string.emergency_contact_phone),
        onCancelClick = onCancelClick,
        onEndClick = onEndClick,
    )
}
