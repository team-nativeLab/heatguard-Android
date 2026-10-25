package com.nativelap.heartguard.viewmodel.emergency

/** 호출과 로그인 세션이 같은 경우에만 소비하는 화면 이동 결과다. */
sealed interface EmergencyEffect {
    val flowGeneration: Long
    val sessionGeneration: Long
    val callId: String

    data class CallStarted(
        override val flowGeneration: Long,
        override val sessionGeneration: Long,
        override val callId: String,
    ) : EmergencyEffect

    data class CallClosed(
        override val flowGeneration: Long,
        override val sessionGeneration: Long,
        override val callId: String,
    ) : EmergencyEffect
}
