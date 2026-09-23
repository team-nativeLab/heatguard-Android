package com.nativelap.heartguard.core.session

/** SessionManager가 상태 변경과 별개로 화면에 일회성으로 알려야 하는 세션 이벤트다. */
sealed interface SessionEvent {
    /** 세션이 만료되어 로그인 화면으로 돌아가야 함을 알린다. */
    data object Expired : SessionEvent
}
