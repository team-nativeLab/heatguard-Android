package com.nativelap.heartguard.domain.emergency.usecase

import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateResult
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.domain.emergency.model.OwnedEmergencyCall
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 로그아웃 직전에 이 기기가 소유한 긴급 호출을 끝낸다. 팀의 최신 호출은 조회하지 않으므로 다른 작업자의 호출은 건드리지 않는다.
 * 접수 전 호출은 취소하고, 관리자가 확인한 호출은 취소할 수 없으므로 종료한다.
 * 요청이 실패하거나 시간 안에 응답하지 않으면 [EndActiveEmergencyCallResult.Unconfirmed]를 돌려주고, 호출자가 로그아웃을 막는다.
 */
class EndActiveEmergencyCallUseCase
    @Inject
    constructor(
        private val emergencyCallRepository: EmergencyCallRepository,
        private val ownedEmergencyCall: OwnedEmergencyCall,
    ) {
        suspend operator fun invoke(): EndActiveEmergencyCallResult {
            val callId = ownedEmergencyCall.current() ?: return EndActiveEmergencyCallResult.NoActiveCall
            val cancelResult = requestStatus(callId, EmergencyCallUpdateStatus.CANCELLED)
            val endResult =
                if (cancelResult == EmergencyCallUpdateResult.InvalidTransition) {
                    // 관리자가 이미 확인한 호출이면 취소가 거부되므로 종료로 다시 보낸다.
                    requestStatus(callId, EmergencyCallUpdateStatus.COMPLETED)
                } else {
                    cancelResult
                }
            return when (endResult) {
                is EmergencyCallUpdateResult.Updated -> {
                    // 응답이 종료 상태가 아니면 서버가 아직 반영하지 않은 것이므로 소유를 유지하고 로그아웃을 막는다.
                    if (endResult.callStatus.state.isClosed()) {
                        ownedEmergencyCall.release(callId)
                        EndActiveEmergencyCallResult.Ended
                    } else {
                        EndActiveEmergencyCallResult.Unconfirmed
                    }
                }

                EmergencyCallUpdateResult.AlreadyClosed -> {
                    ownedEmergencyCall.release(callId)
                    EndActiveEmergencyCallResult.Ended
                }

                EmergencyCallUpdateResult.InvalidTransition,
                EmergencyCallUpdateResult.Failure,
                -> {
                    EndActiveEmergencyCallResult.Unconfirmed
                }
            }
        }

        private fun EmergencyCallState.isClosed(): Boolean =
            this == EmergencyCallState.CANCELLED || this == EmergencyCallState.COMPLETED

        private suspend fun requestStatus(
            callId: String,
            status: EmergencyCallUpdateStatus,
        ): EmergencyCallUpdateResult =
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                emergencyCallRepository.updateEmergencyCallStatus(callId = callId, status = status)
            } ?: EmergencyCallUpdateResult.Failure

        private companion object {
            const val REQUEST_TIMEOUT_MS = 8_000L
        }
    }

/** 로그아웃 전 긴급 호출 종료의 결과다. [Unconfirmed]는 소유 호출이 서버에 남았을 수 있다는 뜻이다. */
enum class EndActiveEmergencyCallResult {
    NoActiveCall,
    Ended,
    Unconfirmed,
}
