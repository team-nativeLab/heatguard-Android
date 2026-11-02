package com.nativelap.heartguard.viewmodel.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallStatus
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.model.OwnedEmergencyCall
import com.nativelap.heartguard.domain.emergency.usecase.GetCurrentEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.ObserveEmergencyCallStatusUseCase
import com.nativelap.heartguard.domain.emergency.usecase.RegisterEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.UpdateEmergencyCallStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/** 긴급 화면(03)과 호출 중 화면(04)이 공유하는 ViewModel이다. HeartGuardNavHost에서 한 번 만들어 두 Route에 넘긴다.
 * 흐름: "긴급 호출하기" → 등록(재시도해도 같은 Idempotency-Key) → 3초 폴링으로 관리자 확인(ACKNOWLEDGED) 반영
 * → 작업자 취소·종료 또는 관리자 종료(CANCELLED/COMPLETED) 시 폴링을 멈추고 [EmergencyEffect.CallClosed]를 보낸다.
 * 실서버의 현재 호출 조회는 종료된 이전 호출도 돌려주므로, 폴링은 이 기기가 시작·이어받은 [EmergencyUiState.callId]만 반영한다. */
@HiltViewModel
class EmergencyViewModel
    @Inject
    constructor(
        private val registerEmergencyCallUseCase: RegisterEmergencyCallUseCase,
        private val observeEmergencyCallStatusUseCase: ObserveEmergencyCallStatusUseCase,
        private val updateEmergencyCallStatusUseCase: UpdateEmergencyCallStatusUseCase,
        private val getCurrentEmergencyCallUseCase: GetCurrentEmergencyCallUseCase,
        private val sessionManager: SessionManager,
        private val clock: Clock,
        private val ownedEmergencyCall: OwnedEmergencyCall,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(EmergencyUiState())
        val uiState: StateFlow<EmergencyUiState> = mutableUiState.asStateFlow()

        private val effectChannel = Channel<EmergencyEffect>(Channel.BUFFERED)
        val effects: Flow<EmergencyEffect> = effectChannel.receiveAsFlow()

        private var flowGeneration = 0L

        private var pollingJob: Job? = null
        private var requestJob: Job? = null
        private var statusUpdateJob: Job? = null

        // 호출 의도 1회에 하나다. 등록이 실패해 다시 누르면 같은 키를 보내 서버에 호출이 중복 생성되지 않게 한다.
        private var pendingIdempotencyKey: String? = null
        private var consecutivePollingFailureCount = 0
        private var dismissAfterRegistration = false

        // 취소·종료를 요청했지만 서버가 아직 확인하지 않은 상태다. 폴링이 호출이 아직 진행 중이라고 알리면 다시 보낸다.
        private var pendingStatusUpdate: PendingStatusUpdate? = null
        private var automaticStatusUpdateAttempts = 0

        // 프로세스 복원으로 새로 만들어진 ViewModel인지 한 번만 확인하기 위한 값이다.
        private var restoredSheetCheckPending = true

        init {
            // 백그라운드에서 세션이 끝나도 3초 폴링이 이전 계정 호출을 계속 조회하지 않도록 함께 정리한다.
            clearStateWhenSessionEnds(sessionManager) {
                reset()
            }
        }

        /** "긴급 호출하기" 버튼에서 호출한다. 이미 진행 중인 호출이 있으면 새로 등록하지 않고 호출 중 화면으로 보낸다. */
        fun startEmergencyCall() {
            val currentState = mutableUiState.value
            if (currentState.isRegistering) {
                return
            }
            if (currentState.callId != null) {
                sendCallStarted()
                return
            }

            val owningFlow = beginFlow()
            val idempotencyKey = pendingIdempotencyKey ?: UUID.randomUUID().toString()
            pendingIdempotencyKey = idempotencyKey
            mutableUiState.value =
                currentState.copy(
                    isRegistering = true,
                    hasRegistrationFailed = false,
                )
            requestJob =
                viewModelScope.launch {
                    val registerResult =
                        withTimeoutOrNull(EMERGENCY_REQUEST_TIMEOUT_MS) {
                            registerEmergencyCallUseCase(
                                idempotencyKey = idempotencyKey,
                                clientOccurredAt = clock.instant(),
                            )
                        } ?: ApiResult.Failure(ApiError.Network)
                    if (!isCurrentFlow(owningFlow)) {
                        return@launch
                    }
                    when (registerResult) {
                        is ApiResult.Success -> {
                            adoptCall(
                                callStatus = registerResult.value,
                                shouldOpenCallScreen = true,
                                isRegisteredByThisDevice = true,
                            )
                        }

                        is ApiResult.Failure -> {
                            if (shouldDismissAfterRegistrationFailure(
                                    dismissRequested = dismissAfterRegistration,
                                    previousMayExist = currentState.registrationMayExist,
                                    error = registerResult.error,
                                )
                            ) {
                                dismissRejectedRegistration()
                            } else if (!registerResult.error.mayHaveCreatedCall()) {
                                showRegistrationFailure(
                                    registrationMayExist =
                                        registrationMayExistAfterFailure(
                                            previousMayExist = currentState.registrationMayExist,
                                            error = registerResult.error,
                                        ),
                                )
                            } else if (registerResult.error.isActiveCallAlreadyExists() || dismissAfterRegistration) {
                                resumeCurrentCall(
                                    shouldOpenCallScreen = true,
                                    onNoActiveCall = ::onRegistrationConfirmedNoCall,
                                    onLookupFailed = { showRegistrationFailure(registrationMayExist = true) },
                                    owningFlow = owningFlow,
                                )
                            } else {
                                showRegistrationFailure(registerResult.error.mayHaveCreatedCall())
                            }
                        }
                    }
                }
        }

        /** 등록 응답을 기다린 뒤 생성된 호출을 취소한다. */
        fun requestDismissDuringRegistration() {
            if (mutableUiState.value.isRegistering) {
                dismissAfterRegistration = true
            } else if (mutableUiState.value.registrationMayExist) {
                dismissAfterRegistration = true
                val owningFlow = beginFlow()
                mutableUiState.value = mutableUiState.value.copy(isRegistering = true)
                requestJob =
                    viewModelScope.launch {
                        resumeCurrentCall(
                            shouldOpenCallScreen = true,
                            onNoActiveCall = ::onRegistrationConfirmedNoCall,
                            onLookupFailed = { showRegistrationFailure(registrationMayExist = true) },
                            owningFlow = owningFlow,
                        )
                    }
            }
        }

        /** 홈 조회에 진행 중인 긴급호출이 있을 때마다 호출한다. 서버의 현재 호출이 진행 중이면 이어받는다.
         * 이어받기에 실패해도 다음 홈 새로고침에서 다시 호출되며, 이미 이어받았으면 아무것도 하지 않는다. */
        fun resumeActiveCall() {
            val currentState = mutableUiState.value
            // 홈 새로고침마다 불릴 수 있으므로 이미 이어받는 중이면 겹쳐 요청하지 않는다.
            if (currentState.callId != null || currentState.isRegistering || requestJob?.isActive == true) {
                return
            }

            val owningFlow = beginFlow()
            requestJob =
                viewModelScope.launch {
                    resumeCurrentCall(
                        shouldOpenCallScreen = false,
                        onNoActiveCall = {},
                        onLookupFailed = {},
                        owningFlow = owningFlow,
                    )
                }
        }

        /** 앱이 백그라운드로 가면 접수 전(ACTIVE) 호출만 취소한다. 관리자가 확인한(ACKNOWLEDGED) 호출은 유지한다.
         * 취소가 실패하면 상태를 보류로 남겨 폴링이 다시 보낸다. */
        fun cancelCallWhenBackgrounded() {
            val currentState = mutableUiState.value
            if (currentState.callId == null || currentState.callState != EmergencyCallState.ACTIVE) {
                return
            }
            updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
        }

        /** 호출 중 화면이 사라질 때 아직 수집되지 않은 종료 이벤트를 버린다. 나중에 다른 화면이 오래된 이벤트로 닫히지 않게 한다. */
        fun discardPendingEffects() {
            while (effectChannel.tryReceive().isSuccess) {
                // 수집되지 않은 이벤트를 버린다.
            }
        }

        /** 프로세스가 복원돼 이 ViewModel이 새로 만들어졌는지 NavHost가 한 번만 확인할 때 쓴다. 처음 한 번만 true를 돌려준다. */
        fun consumeRestoredSheetClose(): Boolean {
            val isFirstCheck = restoredSheetCheckPending
            restoredSheetCheckPending = false
            return isFirstCheck
        }

        /** 취소 또는 종료 버튼에서 호출한다. 서버가 승인한 뒤에만 흐름을 끝낸다. 관리자가 확인한 호출은 취소할 수 없어 종료만 보낸다.
         * 요청이 실패하면 의도를 보류로 남기고, 폴링이 호출이 아직 진행 중이라고 알릴 때 자동으로 다시 보낸다. */
        fun updateCallStatus(requestedStatus: EmergencyCallUpdateStatus) {
            val currentState = mutableUiState.value
            if (currentState.callId == null || currentState.isUpdatingStatus) {
                return
            }
            pendingStatusUpdate =
                PendingStatusUpdate(
                    requestedStatus = requestedStatus,
                    callState = currentState.callState,
                )
            automaticStatusUpdateAttempts = 0
            sendStatusUpdate(requestedStatus)
        }

        private fun sendStatusUpdate(requestedStatus: EmergencyCallUpdateStatus) {
            val currentState = mutableUiState.value
            val currentCallId = currentState.callId ?: return

            val owningFlow = currentFlow()
            val allowedStatus =
                if (currentState.callState == EmergencyCallState.ACKNOWLEDGED) {
                    EmergencyCallUpdateStatus.COMPLETED
                } else {
                    requestedStatus
                }
            mutableUiState.value =
                currentState.copy(
                    isUpdatingStatus = true,
                    statusUpdateFailed = false,
                )
            statusUpdateJob =
                viewModelScope.launch {
                    val updateResult =
                        withTimeoutOrNull(EMERGENCY_REQUEST_TIMEOUT_MS) {
                            updateEmergencyCallStatusUseCase(
                                callId = currentCallId,
                                status = allowedStatus,
                            )
                        } ?: EmergencyCallUpdateResult.Failure
                    if (!isCurrentFlow(owningFlow, currentCallId)) {
                        return@launch
                    }
                    when (updateResult) {
                        is EmergencyCallUpdateResult.Updated -> {
                            // 응답이 종료 상태가 아니면 서버가 아직 취소를 반영하지 않은 것이다. 보류를 남겨 두고 다음 폴링이 상태를 맞춘다.
                            if (updateResult.callStatus.state.isClosed()) {
                                closeCallFlow()
                            } else {
                                mutableUiState.value = mutableUiState.value.copy(isUpdatingStatus = false)
                            }
                        }

                        EmergencyCallUpdateResult.AlreadyClosed -> {
                            closeCallFlow()
                        }

                        EmergencyCallUpdateResult.InvalidTransition -> {
                            // 요청 당시 상태와 달라졌으므로 보류를 버리고, 사용자가 최신 상태를 보고 다시 누르게 한다.
                            pendingStatusUpdate = null
                            refreshCallAfterInvalidTransition(currentCallId, owningFlow)
                        }

                        EmergencyCallUpdateResult.Failure -> {
                            markStatusUpdateFailed()
                        }
                    }
                }
        }

        private fun markStatusUpdateFailed() {
            mutableUiState.value =
                mutableUiState.value.copy(
                    isUpdatingStatus = false,
                    statusUpdateFailed = true,
                )
        }

        // 폴링으로 호출이 아직 진행 중인 것을 확인했을 때만 보류된 요청을 다시 보낸다.
        // 네트워크가 끊겨 폴링도 실패하는 동안에는 보내지 않고, 자동 재시도는 횟수를 제한한다(이후에는 사용자가 누른다).
        // 요청 당시 상태에서 바뀌었다면(예: 관리자가 확인함) 보류를 버린다. 확인된 호출을 보류된 취소 의도로 종료하지 않기 위해서다.
        private fun retryPendingStatusUpdateIfNeeded() {
            val pending = pendingStatusUpdate ?: return
            val currentState = mutableUiState.value
            if (currentState.callId == null || currentState.isUpdatingStatus) {
                return
            }
            if (currentState.callState != pending.callState) {
                pendingStatusUpdate = null
                return
            }
            if (automaticStatusUpdateAttempts >= MAX_AUTOMATIC_STATUS_UPDATE_ATTEMPTS) {
                return
            }
            automaticStatusUpdateAttempts += 1
            sendStatusUpdate(pending.requestedStatus)
        }

        // 요청 사이 관리자가 호출을 확인하는 등 상태가 바뀐 경우다. 최신 상태로 화면을 맞춰 사용자가 다시 누를 수 있게 한다.
        private suspend fun refreshCallAfterInvalidTransition(
            callId: String,
            owningFlow: CallFlow,
        ) {
            val currentCallResult = currentCallWithTimeout()
            if (!isCurrentFlow(owningFlow, callId)) {
                return
            }
            val currentCall =
                when (currentCallResult) {
                    is ApiResult.Success -> {
                        currentCallResult.value
                    }

                    is ApiResult.Failure -> {
                        mutableUiState.value =
                            mutableUiState.value.copy(
                                isUpdatingStatus = false,
                                statusUpdateFailed = true,
                            )
                        return
                    }
                }
            if (currentCall.callId == callId && currentCall.state == EmergencyCallState.UNKNOWN) {
                // 앱이 모르는 새 상태값이면 진행 중인 호출 화면을 닫지 않고, 화면 상태를 유지한 채 다시 시도하게 한다(폴링과 같은 정책).
                mutableUiState.value =
                    mutableUiState.value.copy(
                        isUpdatingStatus = false,
                        statusUpdateFailed = true,
                    )
                return
            }
            val isSameCallInProgress =
                currentCall.callId == callId &&
                    currentCall.state.isInProgress()
            if (!isSameCallInProgress) {
                closeCallFlow()
                return
            }

            mutableUiState.value =
                mutableUiState.value.copy(
                    callState = currentCall.state,
                    isUpdatingStatus = false,
                    statusUpdateFailed = false,
                )
        }

        /** 세션이 끝나 메인 흐름을 벗어날 때 폴링과 진행 중인 요청을 멈추고 상태를 비운다. 서버 호출 상태는 바꾸지 않는다. */
        fun reset() {
            mutableUiState.value.callId?.let(ownedEmergencyCall::release)
            flowGeneration += 1
            pollingJob?.cancel()
            pollingJob = null
            requestJob?.cancel()
            requestJob = null
            statusUpdateJob?.cancel()
            statusUpdateJob = null
            pendingIdempotencyKey = null
            dismissAfterRegistration = false
            pendingStatusUpdate = null
            automaticStatusUpdateAttempts = 0
            consecutivePollingFailureCount = 0
            mutableUiState.value = EmergencyUiState()
            while (effectChannel.tryReceive().isSuccess) {
                // 이전 세션의 이동 결과를 제거한다.
            }
        }

        // 현재 호출을 조회해 진행 중(ACTIVE·ACKNOWLEDGED)이면 이어받고, 아니면 [onNoActiveCall]을 실행한다.
        // [shouldOpenCallScreen]이 false면(앱 시작 시 복원) 화면 이동 없이 상태만 이어받는다.
        private suspend fun resumeCurrentCall(
            shouldOpenCallScreen: Boolean,
            onNoActiveCall: () -> Unit,
            onLookupFailed: () -> Unit,
            owningFlow: CallFlow,
        ) {
            val currentCallResult = currentCallWithTimeout()
            if (!isCurrentFlow(owningFlow)) {
                return
            }
            when (
                val resumeDecision =
                    resolveEmergencyCallResumeDecision(
                        result = currentCallResult,
                    )
            ) {
                EmergencyCallResumeDecision.LookupFailed -> {
                    onLookupFailed()
                }

                is EmergencyCallResumeDecision.ActiveCall -> {
                    adoptCall(
                        callStatus = resumeDecision.callStatus,
                        shouldOpenCallScreen = shouldOpenCallScreen,
                    )
                }

                EmergencyCallResumeDecision.NoActiveCall -> {
                    onNoActiveCall()
                }
            }
        }

        // 현재 호출 조회도 같은 시간 제한을 둔다. 응답이 없으면 확인할 수 없는 실패로 돌려보내 호출한 쪽이 기다리지 않게 한다.
        private suspend fun currentCallWithTimeout(): ApiResult<EmergencyCallStatus> =
            withTimeoutOrNull(EMERGENCY_REQUEST_TIMEOUT_MS) {
                getCurrentEmergencyCallUseCase()
            } ?: ApiResult.Failure(ApiError.Network)

        // [isRegisteredByThisDevice]는 이 기기가 등록 요청으로 만든 호출일 때만 true다. 홈 조회로 이어받은 호출은 팀의 다른 작업자 것일 수 있어 소유로 보지 않는다.
        private fun adoptCall(
            callStatus: EmergencyCallStatus,
            shouldOpenCallScreen: Boolean,
            isRegisteredByThisDevice: Boolean = false,
        ) {
            if (callStatus.state.isClosed() && !callStatus.callId.isNullOrBlank()) {
                pendingIdempotencyKey = null
                onRegistrationConfirmedNoCall()
                return
            }
            if (callStatus.callId.isNullOrBlank() || !callStatus.state.isInProgress()) {
                // 알 수 없는 상태이거나 진행 중인데 식별자가 없으면 호출이 남았을 수 있어 확인되지 않은 실패로 둔다.
                // 호출 없음은 상태가 NONE이거나 식별자 없이 종료된 경우로만 확정한다.
                if (callStatus.state == EmergencyCallState.UNKNOWN || callStatus.state.isInProgress()) {
                    showRegistrationFailure()
                } else {
                    onRegistrationConfirmedNoCall()
                }
                return
            }
            pendingIdempotencyKey = null
            if (isRegisteredByThisDevice) {
                callStatus.callId?.let(ownedEmergencyCall::own)
            }
            mutableUiState.value =
                mutableUiState.value.copy(
                    callId = callStatus.callId,
                    callState = callStatus.state,
                    isRegistering = false,
                    hasRegistrationFailed = false,
                )
            startPolling()
            if (shouldOpenCallScreen) {
                sendCallStarted()
            }
            if (dismissAfterRegistration) {
                dismissAfterRegistration = false
                updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
            }
        }

        // 서버가 진행 중인 호출이 없다고 확인한 경우다. 나가기 요청이 있었다면 홈으로 보내고, 없다면 다시 시도할 수 있게 연다.
        // 확인하지 못한 실패(조회 실패)와 달리 호출이 남아 있지 않으므로 registrationMayExist를 내려 화면이 막히지 않게 한다.
        private fun onRegistrationConfirmedNoCall() {
            if (dismissAfterRegistration) {
                dismissRejectedRegistration()
            } else {
                showRegistrationFailure(registrationMayExist = false)
            }
        }

        private fun showRegistrationFailure() = showRegistrationFailure(registrationMayExist = true)

        private fun showRegistrationFailure(registrationMayExist: Boolean) {
            dismissAfterRegistration = false
            mutableUiState.value =
                mutableUiState.value.copy(
                    isRegistering = false,
                    hasRegistrationFailed = true,
                    registrationMayExist = registrationMayExist,
                )
        }

        private fun startPolling() {
            pollingJob?.cancel()
            consecutivePollingFailureCount = 0
            val owningFlow = currentFlow()
            val owningCallId = mutableUiState.value.callId ?: return
            pollingJob =
                observeEmergencyCallStatusUseCase()
                    .onEach { statusResult ->
                        if (!isCurrentFlow(owningFlow, owningCallId)) {
                            return@onEach
                        }
                        when (statusResult) {
                            is ApiResult.Success -> {
                                applyPolledStatus(statusResult.value)
                            }

                            is ApiResult.Failure -> {
                                // 세션 전환으로 보내지 않은 요청은 연결 실패가 아니므로 실패 횟수에 넣지 않는다.
                                if (statusResult.error != ApiError.SessionChanged) {
                                    countPollingFailure()
                                }
                            }
                        }
                    }.launchIn(viewModelScope)
        }

        private fun applyPolledStatus(polledStatus: EmergencyCallStatus) {
            consecutivePollingFailureCount = 0
            val currentState = mutableUiState.value
            if (currentState.callId != null && polledStatus.callId == null &&
                polledStatus.state == EmergencyCallState.NONE
            ) {
                closeCallFlow()
                return
            }
            // 서버는 가장 최근 호출을 돌려준다. 이 기기의 호출과 다른 호출이면 반영하지 않는다.
            if (polledStatus.callId != currentState.callId) {
                mutableUiState.value = currentState.copy(isConnectionUnstable = false)
                return
            }

            if (polledStatus.state.isClosed()) {
                closeCallFlow()
                return
            }

            mutableUiState.value =
                currentState.copy(
                    callState = polledStatus.state,
                    isConnectionUnstable = false,
                )
            retryPendingStatusUpdateIfNeeded()
        }

        private fun countPollingFailure() {
            consecutivePollingFailureCount += 1
            if (consecutivePollingFailureCount >= UNSTABLE_FAILURE_COUNT) {
                mutableUiState.value = mutableUiState.value.copy(isConnectionUnstable = true)
            }
        }

        private fun dismissRejectedRegistration() {
            reset()
            effectChannel.trySend(
                EmergencyEffect.RegistrationDismissed(
                    flowGeneration = flowGeneration,
                    sessionGeneration = sessionManager.getSnapshot().generation,
                ),
            )
        }

        private fun closeCallFlow() {
            val closedCallId = mutableUiState.value.callId ?: return
            reset()
            effectChannel.trySend(
                EmergencyEffect.CallClosed(
                    flowGeneration = flowGeneration,
                    sessionGeneration = sessionManager.getSnapshot().generation,
                    callId = closedCallId,
                ),
            )
        }

        fun isCurrentEffect(effect: EmergencyEffect): Boolean {
            if (effect.flowGeneration != flowGeneration ||
                effect.sessionGeneration != sessionManager.getSnapshot().generation
            ) {
                return false
            }
            return when (effect) {
                is EmergencyEffect.CallStarted -> effect.callId == mutableUiState.value.callId
                is EmergencyEffect.CallClosed -> mutableUiState.value.callId == null
                is EmergencyEffect.RegistrationDismissed -> mutableUiState.value.callId == null
            }
        }

        private fun sendCallStarted() {
            val startedCallId = mutableUiState.value.callId ?: return
            effectChannel.trySend(
                EmergencyEffect.CallStarted(
                    flowGeneration = flowGeneration,
                    sessionGeneration = sessionManager.getSnapshot().generation,
                    callId = startedCallId,
                ),
            )
        }

        private fun currentFlow(): CallFlow = CallFlow(flowGeneration, sessionManager.getSnapshot().generation)

        private fun beginFlow(): CallFlow {
            flowGeneration += 1
            requestJob?.cancel()
            return currentFlow()
        }

        private fun isCurrentFlow(
            owningFlow: CallFlow,
            callId: String? = null,
        ): Boolean =
            owningFlow.generation == flowGeneration &&
                owningFlow.sessionGeneration == sessionManager.getSnapshot().generation &&
                (callId == null || callId == mutableUiState.value.callId)

        // 사용자가 요청한 취소·종료 의도와, 요청할 때 화면이 보고 있던 호출 상태다.
        private data class PendingStatusUpdate(
            val requestedStatus: EmergencyCallUpdateStatus,
            val callState: EmergencyCallState,
        )

        private data class CallFlow(
            val generation: Long,
            val sessionGeneration: Long,
        )

        private fun EmergencyCallState.isInProgress(): Boolean =
            this == EmergencyCallState.ACTIVE || this == EmergencyCallState.ACKNOWLEDGED

        private fun EmergencyCallState.isClosed(): Boolean =
            this == EmergencyCallState.CANCELLED || this == EmergencyCallState.COMPLETED

        private fun ApiError.isActiveCallAlreadyExists(): Boolean =
            this is ApiError.Http && statusCode == HTTP_CONFLICT && errorCode == ACTIVE_CALL_ALREADY_EXISTS_CODE

        override fun onCleared() {
            pollingJob?.cancel()
        }

        private companion object {
            const val HTTP_CONFLICT = 409
            const val ACTIVE_CALL_ALREADY_EXISTS_CODE = "ACTIVE_CALL_ALREADY_EXISTS"

            // 3초 폴링이 세 번(약 9초) 연속 실패하면 연결이 불안정하다고 알린다.
            const val UNSTABLE_FAILURE_COUNT = 3

            // 등록·취소·조회 요청이 이 시간 안에 응답하지 않으면 실패로 바꾼다. OkHttp 기본 타임아웃(약 10초 이상)을 기다리며 화면이 멈춘 것처럼 보이지 않게 한다.
            const val EMERGENCY_REQUEST_TIMEOUT_MS = 8_000L

            // 보류된 취소·종료의 자동 재시도 상한이다. 3초 폴링 기준 약 30초이며, 그 뒤에는 사용자가 다시 누른다.
            const val MAX_AUTOMATIC_STATUS_UPDATE_ATTEMPTS = 10
        }
    }
