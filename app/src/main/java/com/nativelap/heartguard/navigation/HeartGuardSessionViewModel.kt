package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionEvent
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 앱 최상위 Route(HeartGuardNavHost)가 SessionManager의 인증 상태를 단일 진입점으로 구독하도록 감싼다. */
@HiltViewModel
internal class HeartGuardSessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    val sessionEvents: SharedFlow<SessionEvent> = sessionManager.sessionEvents

    init {
        // 앱 시작 시 저장된 토큰 존재 여부로 초기 인증 상태(Authenticated/Unauthenticated)를 판별한다.
        viewModelScope.launch {
            sessionManager.initialize()
        }
    }

}
