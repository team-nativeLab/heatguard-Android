package com.nativelap.heartguard.core.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** 사용자별 데이터를 가진 ViewModel이 로그아웃·세션 만료·계정 전환 시 이전 사용자 상태를 지우도록 연결한다.
 * NavDisplay가 entry별 ViewModelStore를 쓰지 않아 hiltViewModel()이 Activity 수명으로 남기 때문에,
 * 다른 계정으로 다시 로그인했을 때 이전 작업자의 이름·이메일·입력값이 보이지 않게 하려면 이 연결이 필요하다.
 * 로그아웃→재로그인이 빠르게 일어나 Unauthenticated가 수집 전에 지나가도 세션 세대가 바뀌었으면 정리한다.
 * 앱 시작 시 저장된 세션으로 복원되는 첫 인증은 같은 사용자이므로 정리하지 않는다(입력 초안 복원 유지).
 * ViewModel의 init에서 한 번 호출하며, 수집은 viewModelScope와 함께 끝난다. */
fun ViewModel.clearStateWhenSessionEnds(
    sessionManager: SessionManager,
    clearState: () -> Unit,
) {
    viewModelScope.launch {
        var ownedGeneration: Long? = null
        combine(
            sessionManager.sessionState,
            sessionManager.sessionSnapshot,
        ) { currentSessionState, currentSnapshot ->
            currentSessionState to currentSnapshot.generation
        }.collect { (currentSessionState, currentGeneration) ->
            when (currentSessionState) {
                SessionState.Initializing -> Unit
                SessionState.Unauthenticated -> {
                    ownedGeneration = null
                    clearState()
                }
                SessionState.Authenticated -> {
                    val previousGeneration = ownedGeneration
                    ownedGeneration = currentGeneration
                    if (previousGeneration != null && previousGeneration != currentGeneration) {
                        clearState()
                    }
                }
            }
        }
    }
}
