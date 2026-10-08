package com.nativelap.heartguard.domain.emergency.model

/** 긴급호출 취소·종료 요청의 결과다. 화면은 서버 오류 코드 대신 이 결과로만 분기한다. */
sealed interface EmergencyCallUpdateResult {
    data class Updated(
        val callStatus: EmergencyCallStatus,
    ) : EmergencyCallUpdateResult

    // 관리자 쪽에서 이미 끝난 호출이다(409 EMERGENCY_CALL_CLOSED).
    data object AlreadyClosed : EmergencyCallUpdateResult

    // 현재 상태에서 요청한 상태로 바꿀 수 없다(409 INVALID_STATUS_TRANSITION). 예: 확인된 호출 취소.
    data object InvalidTransition : EmergencyCallUpdateResult

    // 네트워크·서버 오류 등 그 밖의 이유로 실패했다.
    data object Failure : EmergencyCallUpdateResult
}
