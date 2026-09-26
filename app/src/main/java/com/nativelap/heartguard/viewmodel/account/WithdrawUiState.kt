package com.nativelap.heartguard.viewmodel.account

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.account.model.WithdrawReason

/** 회원탈퇴 안내·최종 확인 화면이 함께 보는 입력 상태다.
 * 비밀번호 원문은 data class의 자동 toString으로 노출되지 않도록 여기 두지 않고 입력 여부만 가진다. */
@Immutable
data class WithdrawUiState(
    val selectedReason: WithdrawReason? = null,
    val isAgreed: Boolean = false,
    val isPasswordEntered: Boolean = false,
    val submissionState: WithdrawSubmissionState = WithdrawSubmissionState.Idle,
) {
    // 비밀번호를 입력하고 유의사항에 동의했으며, 이미 요청 중이 아닐 때만 탈퇴하기 버튼을 누를 수 있다.
    val canSubmit: Boolean
        get() = isPasswordEntered &&
            isAgreed &&
            submissionState != WithdrawSubmissionState.Submitting
}

/** 탈퇴 요청의 진행 상태다. 성공 여부를 일회성 이벤트가 아닌 상태로 두어, 공유 ViewModel을 보는 여러 Route 중
 * 엉뚱한 화면이 결과를 소비하는 일을 막는다. */
sealed interface WithdrawSubmissionState {
    data object Idle : WithdrawSubmissionState

    data object Submitting : WithdrawSubmissionState

    data object Succeeded : WithdrawSubmissionState

    data object Failed : WithdrawSubmissionState
}
