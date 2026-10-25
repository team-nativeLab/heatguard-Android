package com.nativelap.heartguard.viewmodel.emergency

import com.nativelap.heartguard.domain.emergency.model.EmergencyCallState

/** 긴급 화면(03)과 호출 중 화면(04)이 함께 쓰는 상태다.
 * [callId]는 이 기기에서 시작했거나 이어받은 진행 중 호출이며, 없으면 취소·종료 버튼을 쓸 수 없다. */
data class EmergencyUiState(
    val callId: String? = null,
    val callState: EmergencyCallState = EmergencyCallState.NONE,
    val isRegistering: Boolean = false,
    val hasRegistrationFailed: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val statusUpdateFailed: Boolean = false,
    // 상태 조회가 연속으로 실패해 관리자 확인 여부를 알 수 없는 상태다. 조회가 한 번 성공하면 해제된다.
    val isConnectionUnstable: Boolean = false,
) {
    val canControlCall: Boolean
        get() = callId != null && !isUpdatingStatus

    val isConnected: Boolean
        get() = callState == EmergencyCallState.ACKNOWLEDGED
}
