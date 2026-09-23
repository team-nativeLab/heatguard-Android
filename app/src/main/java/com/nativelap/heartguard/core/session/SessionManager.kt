package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** 세션 토큰 저장과 인증 상태 공개를 단일 진입점으로 관리한다. */
@Singleton
class SessionManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutableSessionState = MutableStateFlow<SessionState>(SessionState.Initializing)

    val sessionState: StateFlow<SessionState> = mutableSessionState.asStateFlow()

    // 세션 만료처럼 화면이 "지금 막 발생한 일"로 한 번만 반응해야 하는 이벤트를 위한 별도 채널이다.
    // sessionState만으로는 Unauthenticated로의 전환이 로그아웃 직후인지 앱 시작 시 최초 상태인지 구분할 수 없다.
    private val mutableSessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)

    val sessionEvents: SharedFlow<SessionEvent> = mutableSessionEvents.asSharedFlow()

    suspend fun initialize() {
        mutableSessionState.value = withContext(ioDispatcher) {
            if (tokenStorage.readAccessToken().isNullOrBlank()) {
                SessionState.Unauthenticated
            } else {
                SessionState.Authenticated
            }
        }
    }

    suspend fun onLoginSucceeded(sessionToken: SessionToken) {
        withContext(ioDispatcher) {
            tokenStorage.saveAccessToken(sessionToken.accessToken)
        }
        mutableSessionState.value = SessionState.Authenticated
    }

    /** 사용자가 명시적으로 로그아웃하거나(ViewModel), 인증 API가 세션을 복구하지 못했을 때(Authenticator) 호출한다.
     * 저장된 토큰을 지우고 상태를 Unauthenticated로 되돌린 뒤, 화면이 즉시 로그인으로 돌아가도록
     * SessionEvent.Expired를 한 번 알린다. */
    suspend fun expireSession() {
        withContext(ioDispatcher) {
            tokenStorage.clear()
        }
        mutableSessionState.value = SessionState.Unauthenticated
        mutableSessionEvents.emit(SessionEvent.Expired)
    }
}
