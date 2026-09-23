package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionEvent
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.SessionToken
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

    /** 로그인 화면에서 로그인이 성공했을 때 세션을 Authenticated로 전환한다.
     * TODO: 실제 로그인 API가 연결되면 서버가 내려주는 access token으로 교체해야 한다.
     * 지금은 로그인 API 자체가 없어 화면 흐름만 검증 가능한 placeholder token을 사용한다. */
    fun onLoginSucceeded() {
        viewModelScope.launch {
            sessionManager.onLoginSucceeded(SessionToken(accessToken = PLACEHOLDER_ACCESS_TOKEN))
        }
    }

    private companion object {
        const val PLACEHOLDER_ACCESS_TOKEN = "placeholder-access-token"
    }
}
