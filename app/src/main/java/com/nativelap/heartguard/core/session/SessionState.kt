package com.nativelap.heartguard.core.session

/** 앱이 현재 보유한 인증 세션 상태다. */
sealed interface SessionState {
    data object Initializing : SessionState

    data object Authenticated : SessionState

    data object Unauthenticated : SessionState
}
