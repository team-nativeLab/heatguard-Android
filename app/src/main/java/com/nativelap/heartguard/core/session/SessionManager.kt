package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    suspend fun clearSession() {
        withContext(ioDispatcher) {
            tokenStorage.clear()
        }
        mutableSessionState.value = SessionState.Unauthenticated
    }
}
