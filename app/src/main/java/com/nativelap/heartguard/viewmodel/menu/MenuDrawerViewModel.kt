package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.domain.auth.usecase.TeamLogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 메뉴 드로어의 프로필 표시 값과 로그아웃을 담당한다. */
@HiltViewModel
class MenuDrawerViewModel @Inject constructor(
    private val teamLogoutUseCase: TeamLogoutUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    // TODO: 작업자 정보 조회 API가 명세에 아직 없어 모든 값을 비워 두고(화면은 "--" 표시), API가 생기면 연결한다.
    private val _profile = MutableStateFlow(MenuDrawerProfileUiModel())
    val profile: StateFlow<MenuDrawerProfileUiModel> = _profile.asStateFlow()

    private var isLoggingOut = false

    /** 로그아웃 시 서버 세션을 먼저 폐기하고, 요청이 실패하거나 취소돼도 로컬 세션을 종료한다. */
    fun logout() {
        if (isLoggingOut) {
            return
        }

        isLoggingOut = true
        viewModelScope.launch {
            try {
                teamLogoutUseCase()
            } finally {
                try {
                    withContext(NonCancellable) {
                        if (sessionManager.sessionState.value != SessionState.Unauthenticated) {
                            sessionManager.expireSession()
                        }
                    }
                } finally {
                    isLoggingOut = false
                }
            }
        }
    }
}
