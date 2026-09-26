package com.nativelap.heartguard.view.route.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.view.component.account.WithdrawConfirmDialogContent
import com.nativelap.heartguard.view.screen.account.WithdrawDoneScreen
import com.nativelap.heartguard.view.screen.account.WithdrawNoticeScreen
import com.nativelap.heartguard.viewmodel.account.WithdrawNoticeScreenEvent
import com.nativelap.heartguard.viewmodel.account.WithdrawSubmissionState
import com.nativelap.heartguard.viewmodel.account.WithdrawViewModel

/** 회원탈퇴 안내 화면 Route이다. 탈퇴하기를 누르면 최종 확인 다이얼로그([onWithdrawClick])를 띄우고,
 * 요청이 성공하면 [onWithdrawSucceeded]로 완료 화면 전환을 요청한다.
 * 확인 다이얼로그가 떠 있는 동안에도 이 화면은 아래에 계속 그려지므로 성공 감지를 여기 한 곳에서 한다. */
@Composable
internal fun HeartGuardWithdrawNoticeRoute(
    withdrawViewModel: WithdrawViewModel,
    onBackClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onWithdrawSucceeded: () -> Unit,
) {
    // TODO: androidx.lifecycle:lifecycle-runtime-compose 도입이 확정되면 collectAsStateWithLifecycle로 교체한다.
    val uiState by withdrawViewModel.uiState.collectAsState()
    val password by withdrawViewModel.password.collectAsState()
    val latestOnWithdrawSucceeded by rememberUpdatedState(onWithdrawSucceeded)

    LaunchedEffect(uiState.submissionState) {
        if (uiState.submissionState == WithdrawSubmissionState.Succeeded) {
            latestOnWithdrawSucceeded()
        }
    }

    WithdrawNoticeScreen(
        uiState = uiState,
        password = password,
        onEvent = { event ->
            when (event) {
                WithdrawNoticeScreenEvent.BackClicked -> onBackClick()
                is WithdrawNoticeScreenEvent.ReasonSelected -> {
                    withdrawViewModel.selectReason(event.reason)
                }

                is WithdrawNoticeScreenEvent.PasswordChanged -> {
                    withdrawViewModel.updatePassword(event.password)
                }

                is WithdrawNoticeScreenEvent.AgreementChanged -> {
                    withdrawViewModel.updateAgreement(event.isAgreed)
                }

                WithdrawNoticeScreenEvent.WithdrawClicked -> {
                    if (uiState.canSubmit) {
                        onWithdrawClick()
                    }
                }
            }
        },
    )
}

/** 회원탈퇴 최종 확인 다이얼로그 Route이다. 요청이 실패하면 스스로 닫혀 안내 화면의 오류 문구가 보이게 한다. */
@Composable
internal fun HeartGuardWithdrawConfirmRoute(
    withdrawViewModel: WithdrawViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by withdrawViewModel.uiState.collectAsState()
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(uiState.submissionState) {
        if (uiState.submissionState == WithdrawSubmissionState.Failed) {
            latestOnDismiss()
        }
    }

    WithdrawConfirmDialogContent(
        title = stringResource(R.string.withdraw_confirm_title),
        message = stringResource(R.string.withdraw_confirm_message),
        cancelTitle = stringResource(R.string.common_cancel),
        confirmTitle = stringResource(R.string.withdraw_action),
        isSubmitting = uiState.submissionState == WithdrawSubmissionState.Submitting,
        onCancelClick = onDismiss,
        onConfirmClick = withdrawViewModel::withdraw,
        modifier = Modifier.padding(horizontal = HeartGuardSpacing.Section),
    )
}

/** 회원탈퇴 완료 화면 Route이다. 탈퇴가 이미 끝났으므로 확인과 시스템 뒤로가기 모두 세션을 종료해
 * 로그인 화면으로 보낸다(탈퇴 전 화면으로 돌아가지 않게 한다). */
@Composable
internal fun HeartGuardWithdrawDoneRoute(
    withdrawViewModel: WithdrawViewModel,
) {
    BackHandler {
        withdrawViewModel.finishWithdrawal()
    }

    WithdrawDoneScreen(
        onConfirmClick = withdrawViewModel::finishWithdrawal,
    )
}
