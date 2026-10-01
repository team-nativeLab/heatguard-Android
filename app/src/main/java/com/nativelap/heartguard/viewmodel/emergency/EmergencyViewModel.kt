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
import com.nativelap.heartguard.domain.emergency.usecase.GetCurrentEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.ObserveEmergencyCallStatusUseCase
import com.nativelap.heartguard.domain.emergency.usecase.RegisterEmergencyCallUseCase
import com.nativelap.heartguard.domain.emergency.usecase.UpdateEmergencyCallStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
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

/** 긴급 화면(03)과 호출 중 화면(04)이 공유하는 ViewModel이다. HeartGuardNavHost에서 한 번 만들어 두 Route에 넘긴다.
 * 흐름: "긴급 호출하기" → 등록(재시도해도 같은 Idempotency-Key) → 3초 폴링으로 관리자 확인(ACKNOWLEDGED) 반영
 * → 작업자 취소·종료 또는 관리자 종료(CANCELLED/COMPLETED) 시 폴링을 멈추고 [EmergencyEffect.CallClosed]를 보낸다.
 * 실서버의 현재 호출 조회는 종료된 이전 호출도 돌려주므로, 폴링은 이 기기가 시작·이어받은 [EmergencyUiState.callId]만 반영한다. */
@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val registerEmergencyCallUseCase: RegisterEmergencyCallUseCase,
    private val observeEmergencyCallStatusUseCase: ObserveEmergencyCallStatusUseCase,
    private val updateEmergencyCallStatusUseCase: UpdateEmergencyCallStatusUseCase,
    private val getCurrentEmergencyCallUseCase: GetCurrentEmergencyCallUseCase,
    private val sessionManager: SessionManager,
    private val clock: Clock,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<EmergencyEffect>(Channel.BUFFERED)
    val effects: Flow<EmergencyEffect> = effectChannel.receiveAsFlow()

    private var pollingJob: Job? = null
    private var requestJob: Job? = null
    private var statusUpdateJob: Job? = null

    // 호출 의도 1회에 하나다. 등록이 실패해 다시 누르면 같은 키를 보내 서버에 호출이 중복 생성되지 않게 한다.
    private var pendingIdempotencyKey: String? = null
    private var consecutivePollingFailureCount = 0

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
            effectChannel.trySend(EmergencyEffect.CallStarted)
            return
        }

        val idempotencyKey = pendingIdempotencyKey ?: UUID.randomUUID().toString()
        pendingIdempotencyKey = idempotencyKey
        mutableUiState.value = currentState.copy(
            isRegistering = true,
            hasRegistrationFailed = false,
        )
        requestJob = viewModelScope.launch {
            val registerResult = registerEmergencyCallUseCase(
                idempotencyKey = idempotencyKey,
                clientOccurredAt = clock.instant(),
            )
            when (registerResult) {
                is ApiResult.Success -> {
                    adoptCall(
                        callStatus = registerResult.value,
                        shouldOpenCallScreen = true,
                    )
                }

                is ApiResult.Failure -> {
                    if (registerResult.error.isActiveCallAlreadyExists()) {
                        resumeCurrentCall(
                            shouldOpenCallScreen = true,
                            onNoActiveCall = ::showRegistrationFailure,
                        )
                    } else {
                        showRegistrationFailure()
                    }
                }
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

        requestJob = viewModelScope.launch {
            resumeCurrentCall(
                shouldOpenCallScreen = false,
                onNoActiveCall = {},
            )
        }
    }

    /** 취소 또는 종료 버튼에서 호출한다. 서버가 승인한 뒤에만 흐름을 끝낸다. 관리자가 확인한 호출은 취소할 수 없어 종료만 보낸다. */
    fun updateCallStatus(requestedStatus: EmergencyCallUpdateStatus) {
        val currentState = mutableUiState.value
        val currentCallId = currentState.callId ?: return
        if (currentState.isUpdatingStatus) {
            return
        }

        val allowedStatus = if (currentState.callState == EmergencyCallState.ACKNOWLEDGED) {
            EmergencyCallUpdateStatus.COMPLETED
        } else {
            requestedStatus
        }
        mutableUiState.value = currentState.copy(
            isUpdatingStatus = true,
            statusUpdateFailed = false,
        )
        statusUpdateJob = viewModelScope.launch {
            val updateResult = updateEmergencyCallStatusUseCase(
                callId = currentCallId,
                status = allowedStatus,
            )
            when (updateResult) {
                is EmergencyCallUpdateResult.Updated,
                EmergencyCallUpdateResult.AlreadyClosed,
                -> closeCallFlow()

                EmergencyCallUpdateResult.InvalidTransition -> refreshCallAfterInvalidTransition(currentCallId)

                EmergencyCallUpdateResult.Failure -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        isUpdatingStatus = false,
                        statusUpdateFailed = true,
                    )
                }
            }
        }
    }

    // 요청 사이 관리자가 호출을 확인하는 등 상태가 바뀐 경우다. 최신 상태로 화면을 맞춰 사용자가 다시 누를 수 있게 한다.
    private suspend fun refreshCallAfterInvalidTransition(callId: String) {
        val currentCall = when (val currentCallResult = getCurrentEmergencyCallUseCase()) {
            is ApiResult.Success -> currentCallResult.value
            is ApiResult.Failure -> {
                mutableUiState.value = mutableUiState.value.copy(
                    isUpdatingStatus = false,
                    statusUpdateFailed = true,
                )
                return
            }
        }
        if (currentCall.callId == callId && currentCall.state == EmergencyCallState.UNKNOWN) {
            // 앱이 모르는 새 상태값이면 진행 중인 호출 화면을 닫지 않고, 화면 상태를 유지한 채 다시 시도하게 한다(폴링과 같은 정책).
            mutableUiState.value = mutableUiState.value.copy(
                isUpdatingStatus = false,
                statusUpdateFailed = true,
            )
            return
        }
        val isSameCallInProgress = currentCall.callId == callId &&
            currentCall.state.isInProgress()
        if (!isSameCallInProgress) {
            closeCallFlow()
            return
        }

        mutableUiState.value = mutableUiState.value.copy(
            callState = currentCall.state,
            isUpdatingStatus = false,
            statusUpdateFailed = false,
        )
    }

    /** 세션이 끝나 메인 흐름을 벗어날 때 폴링과 진행 중인 요청을 멈추고 상태를 비운다. 서버 호출 상태는 바꾸지 않는다. */
    fun reset() {
        pollingJob?.cancel()
        pollingJob = null
        requestJob?.cancel()
        requestJob = null
        statusUpdateJob?.cancel()
        statusUpdateJob = null
        pendingIdempotencyKey = null
        consecutivePollingFailureCount = 0
        mutableUiState.value = EmergencyUiState()
    }

    // 현재 호출을 조회해 진행 중(ACTIVE·ACKNOWLEDGED)이면 이어받고, 아니면 [onNoActiveCall]을 실행한다.
    // [shouldOpenCallScreen]이 false면(앱 시작 시 복원) 화면 이동 없이 상태만 이어받는다.
    private suspend fun resumeCurrentCall(
        shouldOpenCallScreen: Boolean,
        onNoActiveCall: () -> Unit,
    ) {
        val currentCallResult = getCurrentEmergencyCallUseCase()
        val currentCall = (currentCallResult as? ApiResult.Success)?.value
        if (currentCall != null && currentCall.callId != null && currentCall.state.isInProgress()) {
            adoptCall(
                callStatus = currentCall,
                shouldOpenCallScreen = shouldOpenCallScreen,
            )
        } else {
            onNoActiveCall()
        }
    }

    private fun adoptCall(
        callStatus: EmergencyCallStatus,
        shouldOpenCallScreen: Boolean,
    ) {
        pendingIdempotencyKey = null
        mutableUiState.value = mutableUiState.value.copy(
            callId = callStatus.callId,
            callState = callStatus.state,
            isRegistering = false,
            hasRegistrationFailed = false,
        )
        startPolling()
        if (shouldOpenCallScreen) {
            effectChannel.trySend(EmergencyEffect.CallStarted)
        }
    }

    private fun showRegistrationFailure() {
        mutableUiState.value = mutableUiState.value.copy(
            isRegistering = false,
            hasRegistrationFailed = true,
        )
    }

    private fun startPolling() {
        pollingJob?.cancel()
        consecutivePollingFailureCount = 0
        pollingJob = observeEmergencyCallStatusUseCase()
            .onEach { statusResult ->
                when (statusResult) {
                    is ApiResult.Success -> applyPolledStatus(statusResult.value)
                    is ApiResult.Failure -> {
                        // 세션 전환으로 보내지 않은 요청은 연결 실패가 아니므로 실패 횟수에 넣지 않는다.
                        if (statusResult.error != ApiError.SessionChanged) {
                            countPollingFailure()
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
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

        mutableUiState.value = currentState.copy(
            callState = polledStatus.state,
            isConnectionUnstable = false,
        )
    }

    private fun countPollingFailure() {
        consecutivePollingFailureCount += 1
        if (consecutivePollingFailureCount >= UNSTABLE_FAILURE_COUNT) {
            mutableUiState.value = mutableUiState.value.copy(isConnectionUnstable = true)
        }
    }

    private fun closeCallFlow() {
        pollingJob?.cancel()
        pollingJob = null
        pendingIdempotencyKey = null
        mutableUiState.value = EmergencyUiState()
        effectChannel.trySend(EmergencyEffect.CallClosed)
    }

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
    }
}
