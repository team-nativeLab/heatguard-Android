package com.nativelap.heartguard.viewmodel.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 메뉴 드로어의 프로필 표시 값과 로그아웃을 담당한다. */
@HiltViewModel
class MenuDrawerViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {
    // 프로필 조회 응답 계약이 보완되기 전까지는 Figma의 예시 계정값을 사용자 정보처럼 표시하지 않는다.
    private val _profile = MutableStateFlow(MenuDrawerProfileUiModel())
    val profile: StateFlow<MenuDrawerProfileUiModel> = _profile.asStateFlow()

    private var isLoggingOut = false

    /** 드로어의 로그아웃 항목을 눌렀을 때 호출한다. 저장된 토큰을 지우고 세션을 종료하면
     * HeartGuardNavHost가 인증 상태 변화를 받아 로그인 화면으로 전환한다. 연속 탭은 한 번만 처리한다. */
    fun logout() {
        if (isLoggingOut) {
            return
        }

        isLoggingOut = true
        viewModelScope.launch {
            try {
                sessionManager.expireSession()
            } finally {
                isLoggingOut = false
            }
        }
    }
}
