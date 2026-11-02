package com.nativelap.heartguard.view.route.emergency

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.isValidPhoneNumber
import com.nativelap.heartguard.core.util.rememberPhoneDialLauncher
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.view.screen.emergency.CallingScreen
import com.nativelap.heartguard.view.screen.emergency.EmergencyScreen
import com.nativelap.heartguard.viewmodel.emergency.EmergencyEffect
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel
import com.nativelap.heartguard.viewmodel.home.HomeViewModel

/** 긴급 화면(03)이다. "긴급 호출하기"를 눌러야 호출을 등록하고, 등록되면 호출 중 화면으로 이동한다.
 * 이미 진행 중인 호출이 있으면 NavHost가 이 화면 대신 호출 중 화면으로 보낸다.
 * 연락처는 홈 조회의 본사 전화번호(company.phone)이며, 없으면 "--"로 두고 전화 걸기를 막는다. */
@Composable
internal fun HeartGuardEmergencyRoute(
    emergencyViewModel: EmergencyViewModel,
    homeViewModel: HomeViewModel,
    onCallStarted: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val uiState by emergencyViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val headquartersPhoneNumber = siteStatus.headquartersPhoneNumber
    val currentOnCallStarted by rememberUpdatedState(onCallStarted)
    val currentOnCancelClick by rememberUpdatedState(onCancelClick)

    CollectEmergencyEffects(
        emergencyViewModel = emergencyViewModel,
        onCallStarted = { currentOnCallStarted() },
        onCallClosed = { currentOnCancelClick() },
    )

    val requestDismiss = {
        if (uiState.callId != null) {
            emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        } else if (uiState.isRegistering || uiState.registrationMayExist) {
            emergencyViewModel.requestDismissDuringRegistration()
        } else {
            onCancelClick()
        }
    }
    BackHandler(onBack = requestDismiss)

    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = valueOrEmptyText(headquartersPhoneNumber),
        isContactCallEnabled = headquartersPhoneNumber?.isValidPhoneNumber() == true,
        isRegistering = uiState.isRegistering,
        hasRegistrationFailed = uiState.hasRegistrationFailed,
        onCallClick = emergencyViewModel::startEmergencyCall,
        onCancelClick = requestDismiss,
        onContactClick = {
            headquartersPhoneNumber?.let(dialPhoneNumber)
        },
    )
}

/** 호출 중 화면(04)이다. 폴링한 서버 상태로 관리자 확인(연결됨)을 보여주고, 취소·종료 또는 관리자 종료 시 홈으로 돌아간다.
 * 호출 식별자가 없으면(이어받기 전) 취소·종료 버튼을 누를 수 없다. */
@Composable
internal fun HeartGuardCallingRoute(
    emergencyViewModel: EmergencyViewModel,
    homeViewModel: HomeViewModel,
    onCallClosed: () -> Unit,
) {
    val uiState by emergencyViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val headquartersPhoneNumber = siteStatus.headquartersPhoneNumber
    val currentOnCallClosed by rememberUpdatedState(onCallClosed)

    CollectEmergencyEffects(
        emergencyViewModel = emergencyViewModel,
        onCallStarted = {},
        onCallClosed = { currentOnCallClosed() },
    )

    // 앱이 백그라운드로 가면 진행 중인 호출을 취소해 서버에 호출이 남지 않게 한다.
    // 회전·다크모드 같은 구성 변경으로 Activity가 다시 만들어질 때는 ON_STOP이 와도 취소하지 않는다.
    val hostActivity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, emergencyViewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP && hostActivity?.isChangingConfigurations != true) {
                    emergencyViewModel.cancelCallWhenBackgrounded()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 화면이 실제로 사라질 때만(회전 제외) 아직 수집되지 않은 종료 이벤트를 버린다.
            if (hostActivity?.isChangingConfigurations != true) {
                emergencyViewModel.discardPendingEffects()
            }
        }
    }

    BackHandler {
        if (uiState.callId == null) {
            // 호출 식별자가 없으면 취소할 수 없으므로(버튼 비활성) 뒤로가기로 호출 중 화면을 나간다.
            currentOnCallClosed()
        } else {
            emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        }
    }

    CallingScreen(
        isConnected = uiState.isConnected,
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = valueOrEmptyText(headquartersPhoneNumber),
        isContactCallEnabled = headquartersPhoneNumber?.isValidPhoneNumber() == true,
        isUpdatingStatus = !uiState.canControlCall,
        statusUpdateError = uiState.statusUpdateFailed,
        isConnectionUnstable = uiState.isConnectionUnstable,
        onCancelClick = {
            emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        },
        onEndClick = {
            emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.COMPLETED)
        },
        onContactClick = {
            headquartersPhoneNumber?.let(dialPhoneNumber)
        },
    )
}

// 긴급호출 일회성 결과는 화면이 보이는 동안에만 수집한다. 두 Route 중 화면에 올라온 쪽 하나만 수집한다.
@Composable
private fun CollectEmergencyEffects(
    emergencyViewModel: EmergencyViewModel,
    onCallStarted: () -> Unit,
    onCallClosed: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCallStarted by rememberUpdatedState(onCallStarted)
    val currentOnCallClosed by rememberUpdatedState(onCallClosed)

    LaunchedEffect(emergencyViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            emergencyViewModel.effects.collect { emergencyEffect ->
                if (!emergencyViewModel.isCurrentEffect(emergencyEffect)) {
                    return@collect
                }
                when (emergencyEffect) {
                    is EmergencyEffect.CallStarted -> currentOnCallStarted()

                    is EmergencyEffect.CallClosed,
                    is EmergencyEffect.RegistrationDismissed,
                    -> currentOnCallClosed()
                }
            }
        }
    }
}
