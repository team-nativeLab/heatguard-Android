package com.nativelap.heartguard.viewmodel.account

import com.nativelap.heartguard.domain.account.model.WithdrawReason

/** 회원탈퇴 안내 화면에서 Route로 전달하는 사용자 이벤트다. */
sealed interface WithdrawNoticeScreenEvent {
    data object BackClicked : WithdrawNoticeScreenEvent

    data class ReasonSelected(val reason: WithdrawReason?) : WithdrawNoticeScreenEvent

    data class PasswordChanged(val password: String) : WithdrawNoticeScreenEvent

    data class AgreementChanged(val isAgreed: Boolean) : WithdrawNoticeScreenEvent

    data object WithdrawClicked : WithdrawNoticeScreenEvent
}
