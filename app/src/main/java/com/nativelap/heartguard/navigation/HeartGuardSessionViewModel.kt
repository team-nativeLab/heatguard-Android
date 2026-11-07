package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionEvent
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.domain.profile.model.WorkerProfile
import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase
import com.nativelap.heartguard.domain.site.usecase.GetAppLinkedSitesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** 앱 최상위 Route(HeartGuardNavHost)가 SessionManager의 인증 상태를 단일 진입점으로 구독하도록 감싼다. */
@HiltViewModel
internal class HeartGuardSessionViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val getWorkerProfileUseCase: GetWorkerProfileUseCase,
        private val getAppLinkedSitesUseCase: GetAppLinkedSitesUseCase,
    ) : ViewModel() {
        val sessionState: StateFlow<SessionState> = sessionManager.sessionState

        val sessionRole: StateFlow<SessionRole?> =
            sessionManager.sessionSnapshot
                .map { sessionSnapshot -> sessionSnapshot.role }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = sessionManager.getSnapshot().role,
                )

        val sessionEvents: SharedFlow<SessionEvent> = sessionManager.sessionEvents

        val sessionSnapshot: StateFlow<SessionSnapshot> = sessionManager.sessionSnapshot

        private val mutableReadyGeneration = MutableStateFlow<Long?>(null)
        val readyGeneration: StateFlow<Long?> = mutableReadyGeneration.asStateFlow()

        val isStartupCheckComplete: StateFlow<Boolean> =
            combine(sessionSnapshot, readyGeneration) { snapshot, ready -> snapshot.generation == ready }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        // 토큰은 화면에 노출하지 않고, 계정 전환마다 메인 흐름을 새로 만들 기준인 세대만 공개한다.
        val sessionGeneration: StateFlow<Long> =
            sessionSnapshot
                .map { sessionSnapshot -> sessionSnapshot.generation }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = sessionManager.getSnapshot().generation,
                )

        fun expireTeamMemberSession() {
            viewModelScope.launch {
                val currentSnapshot: SessionSnapshot = sessionManager.getSnapshot()
                if (currentSnapshot.role == SessionRole.TEAM_MEMBER) {
                    sessionManager.expireSession(currentSnapshot.generation)
                }
            }
        }

        fun expireLegacyAppAccountSession() {
            viewModelScope.launch {
                val currentSnapshot: SessionSnapshot = sessionManager.getSnapshot()
                if (currentSnapshot.role == SessionRole.APP_ACCOUNT) {
                    sessionManager.expireSession(currentSnapshot.generation)
                }
            }
        }

        init {
            // 새 로그인과 복원 세션 모두 memberships를 확인한 뒤 홈을 연다. 오류는 오프라인 홈 사용을 막지 않는다.
            viewModelScope.launch {
                sessionManager.initialize()
                sessionManager.sessionSnapshot.collectLatest { sessionSnapshot ->
                    if (sessionSnapshot.role == SessionRole.TEAM_MEMBER &&
                        !sessionSnapshot.accessToken.isNullOrBlank()
                    ) {
                        if (sessionManager.isRestoredSession()) checkRestoredSessionOnce()
                        if (sessionManager.getSnapshot() == sessionSnapshot) {
                            withTimeoutOrNull(STARTUP_SESSION_CHECK_TIMEOUT_MS) {
                                getAppLinkedSitesUseCase(sessionSnapshot.generation)
                            }
                        }
                    }
                    if (sessionManager.getSnapshot() == sessionSnapshot) {
                        mutableReadyGeneration.value = sessionSnapshot.generation
                    }
                }
            }
        }

        // 저장 토큰이 서버에서 이미 거부된 경우 홈을 띄우기 전에 로그인 화면으로 보낸다.
        // 네트워크 실패·서버 오류·시간 초과는 오프라인 사용을 막지 않도록 그대로 홈으로 진행한다.
        private suspend fun checkRestoredSessionOnce() {
            val restoredSnapshot: SessionSnapshot = sessionManager.getSnapshot()
            val shouldCheckRestoredSession =
                sessionManager.isRestoredSession() &&
                    restoredSnapshot.role == SessionRole.TEAM_MEMBER &&
                    !sessionManager.isStartupCheckDone(restoredSnapshot.generation)
            if (!shouldCheckRestoredSession) {
                return
            }
            val profileResult: ApiResult<WorkerProfile>? =
                withTimeoutOrNull(STARTUP_SESSION_CHECK_TIMEOUT_MS) {
                    getWorkerProfileUseCase(restoredSnapshot.generation)
                }
            sessionManager.markStartupCheckDone(restoredSnapshot.generation)
            // 401은 BearerTokenAuthenticator가 이미 세션을 끝내므로 앱 서버가 거부한 403만 여기서 만료한다.
            if (profileResult.isRejectedByAppServerAsForbidden()) {
                sessionManager.expireSession(restoredSnapshot.generation)
            }
        }

        // errorCode가 없는 403은 CDN·WAF 같은 중간 계층 응답일 수 있어 세션 거부로 보지 않는다.
        private fun ApiResult<WorkerProfile>?.isRejectedByAppServerAsForbidden(): Boolean {
            if (this !is ApiResult.Failure) {
                return false
            }
            val failureError = error
            return failureError is ApiError.Http &&
                failureError.statusCode == HTTP_FORBIDDEN_STATUS_CODE &&
                !failureError.errorCode.isNullOrBlank()
        }

        private companion object {
            const val STARTUP_SESSION_CHECK_TIMEOUT_MS = 5_000L
            const val HTTP_FORBIDDEN_STATUS_CODE = 403
        }
    }
