package com.nativelap.heartguard.view.route.emergency

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.isValidPhoneNumber
import com.nativelap.heartguard.core.util.rememberPhoneCallLauncher
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

    // 등록 요청 중에는 뒤로가기로 시트를 닫지 않는다(응답 전에 닫으면 호출 결과를 놓친다).
    BackHandler {
        if (!uiState.isRegistering) {
            onCancelClick()
        }
    }

    EmergencyScreen(
        contactName = stringResource(R.string.emergency_contact_name),
        phoneNumber = valueOrEmptyText(headquartersPhoneNumber),
        isContactCallEnabled = headquartersPhoneNumber?.isValidPhoneNumber() == true,
        isRegistering = uiState.isRegistering,
        hasRegistrationFailed = uiState.hasRegistrationFailed,
        onCallClick = emergencyViewModel::startEmergencyCall,
        onCancelClick = onCancelClick,
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
    val context = LocalContext.current
    val uiState by emergencyViewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val callPhoneNumber = rememberPhoneCallLauncher()
    val dialPhoneNumber = rememberPhoneDialLauncher()
    val headquartersPhoneNumber = siteStatus.headquartersPhoneNumber
    val callFailedMessage = stringResource(R.string.home_call_failed)
    var lastAutoCallAttemptedCallId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var pendingPermissionCallId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val latestUiState by rememberUpdatedState(uiState)
    val latestHeadquartersPhoneNumber by rememberUpdatedState(headquartersPhoneNumber)
    val latestCallPhoneNumber by rememberUpdatedState(callPhoneNumber)
    val latestDialPhoneNumber by rememberUpdatedState(dialPhoneNumber)
    val latestContext by rememberUpdatedState(context)
    val latestCallFailedMessage by rememberUpdatedState(callFailedMessage)
    val attemptPhoneCall: (String, Boolean) -> Unit = { expectedCallId, hasCallPermission ->
        val currentUiState = latestUiState
        val currentPhoneNumber = latestHeadquartersPhoneNumber
        val isSameCallStillConnected =
            currentUiState.callId == expectedCallId &&
                canAttemptEmergencyAutoCall(
                    callId = currentUiState.callId,
                    autoCallEligibleCallId = currentUiState.autoCallEligibleCallId,
                    isConnected = currentUiState.isConnected,
                    canControlCall = currentUiState.canControlCall,
                )
        val usablePhoneNumber =
            currentPhoneNumber?.takeIf { phoneNumber ->
                phoneNumber.isValidPhoneNumber()
            }

        if (isSameCallStillConnected && usablePhoneNumber != null) {
            val didStartDirectCall =
                hasCallPermission && latestCallPhoneNumber(usablePhoneNumber)
            val didOpenPhoneApp =
                didStartDirectCall || latestDialPhoneNumber(usablePhoneNumber)

            if (!didOpenPhoneApp) {
                Toast.makeText(latestContext, latestCallFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val latestAttemptPhoneCall by rememberUpdatedState(attemptPhoneCall)
    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isPermissionGranted ->
            val requestedCallId = pendingPermissionCallId
            pendingPermissionCallId = null

            if (requestedCallId != null) {
                latestAttemptPhoneCall(requestedCallId, isPermissionGranted)
            }
        }
    val currentOnCallClosed by rememberUpdatedState(onCallClosed)

    LaunchedEffect(
        uiState.callId,
        uiState.autoCallEligibleCallId,
        uiState.isConnected,
        uiState.canControlCall,
        headquartersPhoneNumber,
        pendingPermissionCallId,
    ) {
        val activeCallId = uiState.callId
        val shouldRequestOrPlaceCall =
            shouldStartEmergencyAutoCall(
                callId = activeCallId,
                autoCallEligibleCallId = uiState.autoCallEligibleCallId,
                isConnected = uiState.isConnected,
                canControlCall = uiState.canControlCall,
                phoneNumber = headquartersPhoneNumber,
                lastAttemptedCallId = lastAutoCallAttemptedCallId,
            )

        if (!shouldRequestOrPlaceCall || pendingPermissionCallId != null) {
            return@LaunchedEffect
        }

        val callId = activeCallId ?: return@LaunchedEffect
        lastAutoCallAttemptedCallId = callId

        val hasCallPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE,
            ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            attemptPhoneCall(callId, true)
        } else {
            pendingPermissionCallId = callId
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    CollectEmergencyEffects(
        emergencyViewModel = emergencyViewModel,
        onCallStarted = {},
        onCallClosed = { currentOnCallClosed() },
    )

    // 진행 중 호출은 실수로 닫히지 않도록 시스템 뒤로가기를 소비한다.
    BackHandler(enabled = true) {}

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
                    is EmergencyEffect.CallClosed -> currentOnCallClosed()
                }
            }
        }
    }
}
