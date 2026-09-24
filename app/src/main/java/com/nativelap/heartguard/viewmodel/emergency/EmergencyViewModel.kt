package com.nativelap.heartguard.viewmodel.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.emergency.usecase.ObserveEmergencyCallStatusUseCase
import com.nativelap.heartguard.domain.emergency.usecase.RegisterEmergencyCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Emergency 화면과 Calling 화면이 함께 공유하는 ViewModel이다(android-navigation SKILL
 * '화면 간 ViewModel 공유' 패턴, [com.nativelap.heartguard.navigation.HeartGuardNavHost]에서
 * 수동 ViewModelStoreOwner로 인스턴스를 하나만 만들어 두 Route에 명시적으로 전달한다).
 * 긴급호출을 등록한 뒤 3초 간격으로 상태를 폴링해, 관리자가 확인(ACKNOWLEDGED)하면 Calling
 * 화면이 "연결됨" 상태를 보여줄 수 있게 한다. */
@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val registerEmergencyCallUseCase: RegisterEmergencyCallUseCase,
    private val observeEmergencyCallStatusUseCase: ObserveEmergencyCallStatusUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = mutableUiState.asStateFlow()

    private var pollingJob: Job? = null

    /** Emergency 화면 진입 시 1회 호출한다. 이미 ACTIVE 호출이 있으면 서버가
     * 409 ACTIVE_CALL_ALREADY_EXISTS로 응답하는데, 그 경우도 "이미 호출 중"인 정상 상태이므로
     * 등록 성공 여부와 무관하게 상태 폴링은 항상 시작한다. */
    fun registerEmergencyCallIfNeeded() {
        if (pollingJob != null) {
            return
        }
        viewModelScope.launch {
            registerEmergencyCallUseCase()
            startObservingStatus()
        }
    }

    private fun startObservingStatus() {
        pollingJob = observeEmergencyCallStatusUseCase()
            .onEach { result ->
                if (result is ApiResult.Success) {
                    mutableUiState.value = EmergencyUiState(status = result.value)
                }
            }
            .launchIn(viewModelScope)
    }

    /** 긴급호출 흐름을 완전히 벗어날 때(홈으로 돌아갈 때) 호출해 폴링을 멈추고 상태를 초기화한다.
     * 그러지 않으면 다음에 다시 Emergency에 진입했을 때 이전 폴링 상태가 남아 있게 된다. */
    fun reset() {
        pollingJob?.cancel()
        pollingJob = null
        mutableUiState.value = EmergencyUiState()
    }

    override fun onCleared() {
        pollingJob?.cancel()
    }
}
