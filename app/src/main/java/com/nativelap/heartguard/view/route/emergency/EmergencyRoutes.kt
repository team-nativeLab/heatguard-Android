package com.nativelap.heartguard.view.route.emergency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel

/** 긴급 화면의 현장 관리자 연락처와 호출 취소 Navigation callback을 연결한다.
 * 화면 진입 시 [emergencyViewModel]로 긴급호출을 등록하고 상태 폴링을 시작한다.
 * 연락처는 팀 현장페이지 응답의 관리자 번호(site.managerPhone)이며, 받지 못했으면 "--"로 두고 전화 걸기를 막는다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    emergencyViewModel: EmergencyViewModel,
    homeViewModel: HomeViewModel,
    onCallClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    LaunchedEffect(Unit) {
        emergencyViewModel.registerEmergencyCallIfNeeded()
    }

    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val managerPhoneNumber = siteStatus.managerPhoneNumber

    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = valueOrEmptyText(managerPhoneNumber),
        isContactCallEnabled = managerPhoneNumber != null,
        onCallClick = onCallClick,
        // 호출 취소는 진행 중인 알림·폴링을 정리한 뒤에만 이전 화면으로 돌아간다.
        onCancelClick = {
            emergencyViewModel.reset()
            onCancelClick()
        },
        onContactClick = {
            managerPhoneNumber?.let(dialPhoneNumber)
        },
    )
}

/** 호출 대기 Screen을 표시하고 취소·종료 목적지 callback을 전달한다.
 * [emergencyViewModel]이 폴링한 실제 상태(ACKNOWLEDGED)로 "연결됨" 화면을 보여준다. */
@Composable
internal fun HeartGuardCallingRoute(
    emergencyViewModel: EmergencyViewModel,
    homeViewModel: HomeViewModel,
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    val uiState by emergencyViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val managerPhoneNumber = siteStatus.managerPhoneNumber

    CallingScreen(
        isConnected = uiState.status.state == EmergencyCallState.ACKNOWLEDGED,
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = valueOrEmptyText(managerPhoneNumber),
        isContactCallEnabled = managerPhoneNumber != null,
        onCancelClick = onCancelClick,
        // 통화 종료는 폴링을 정리한 뒤에만 홈으로 돌아간다.
        onEndClick = {
            emergencyViewModel.reset()
            onEndClick()
        },
        onContactClick = {
            managerPhoneNumber?.let(dialPhoneNumber)
        },
    )
}
