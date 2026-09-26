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
    // TODO: 사용자 정보 조회 API가 생기면 서버 값으로 교체한다. 지금은 로그인 API도 placeholder라
    // 사용자 정보를 받을 곳이 없어 Figma 16_메뉴_드로어의 예시 값을 임시로 보여준다.
    private val _profile = MutableStateFlow(
        MenuDrawerProfileUiModel(
            userName = PLACEHOLDER_USER_NAME,
            companyName = PLACEHOLDER_COMPANY_NAME,
            jobTitle = PLACEHOLDER_JOB_TITLE,
            email = PLACEHOLDER_EMAIL,
        ),
    )
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

    private companion object {
        const val PLACEHOLDER_USER_NAME = "김현장"
        const val PLACEHOLDER_COMPANY_NAME = "이음산업건설"
        const val PLACEHOLDER_JOB_TITLE = "현장작업자"
        const val PLACEHOLDER_EMAIL = "worker@ieum.co.kr"
    }
}
