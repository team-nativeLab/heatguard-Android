package com.nativelap.heartguard.core.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** 사용자별 데이터를 가진 ViewModel이 로그아웃·세션 만료 시 이전 사용자 상태를 지우도록 연결한다.
 * NavDisplay가 entry별 ViewModelStore를 쓰지 않아 hiltViewModel()이 Activity 수명으로 남기 때문에,
 * 다른 계정으로 다시 로그인했을 때 이전 작업자의 이름·이메일·입력값이 보이지 않게 하려면 이 연결이 필요하다.
 * ViewModel의 init에서 한 번 호출하며, 수집은 viewModelScope와 함께 끝난다. */
fun ViewModel.clearStateWhenSessionEnds(
    sessionManager: SessionManager,
    clearState: () -> Unit,
) {
    viewModelScope.launch {
        sessionManager.sessionState.collect { currentSessionState ->
            if (currentSessionState == SessionState.Unauthenticated) {
                clearState()
            }
        }
    }
}
