package com.nativelap.heartguard.viewmodel.emergency

/** 긴급호출 흐름에서 화면 이동처럼 한 번만 처리해야 하는 결과다. */
sealed interface EmergencyEffect {
    // 호출이 등록되었거나 진행 중인 호출을 이어받았다. 호출 중 화면으로 이동한다.
    data object CallStarted : EmergencyEffect

    // 작업자가 취소·종료했거나 관리자 쪽에서 호출을 끝냈다. 홈으로 돌아간다.
    data object CallClosed : EmergencyEffect
}
