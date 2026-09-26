package com.nativelap.heartguard.view.screen.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.account.WithdrawAgreementRow
import com.nativelap.heartguard.view.component.account.WithdrawDangerButton
import com.nativelap.heartguard.view.component.account.WithdrawHeadline
import com.nativelap.heartguard.view.component.account.WithdrawNoticeCard
import com.nativelap.heartguard.view.component.account.WithdrawReasonCard
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.viewmodel.account.WithdrawNoticeScreenEvent
import com.nativelap.heartguard.viewmodel.account.WithdrawSubmissionState
import com.nativelap.heartguard.viewmodel.account.WithdrawUiState

/** Figma 17_회원탈퇴_안내 화면이다. 유의사항·탈퇴 사유·비밀번호·동의를 받고,
 * 조건을 모두 채우면 하단 탈퇴하기 버튼을 활성화한다. 직전 요청이 실패했으면 버튼 위에 오류 안내를 보여준다. */
@Composable
fun WithdrawNoticeScreen(
    uiState: WithdrawUiState,
    password: String,
    onEvent: (WithdrawNoticeScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.withdraw_title),
                backContentDescription = stringResource(R.string.withdraw_back_description),
                onBackClick = { onEvent(WithdrawNoticeScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Item),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = HeartGuardSpacing.AccountContentHorizontal,
                        vertical = HeartGuardSpacing.Section,
                    ),
            ) {
                WithdrawHeadline(
                    title = stringResource(R.string.withdraw_notice_headline),
                    subtitle = stringResource(R.string.withdraw_notice_subtitle),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

                WithdrawNoticeCard(
                    title = stringResource(R.string.withdraw_notice_card_title),
                    noticeLines = listOf(
                        stringResource(R.string.withdraw_notice_account_deleted),
                        stringResource(R.string.withdraw_notice_records_retained),
                        stringResource(R.string.withdraw_notice_records_not_linked),
                    ),
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

                WithdrawReasonCard(
                    selectedReason = uiState.selectedReason,
                    onReasonSelect = { reason ->
                        onEvent(WithdrawNoticeScreenEvent.ReasonSelected(reason))
                    },
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Card))

                AuthTextField(
                    label = stringResource(R.string.withdraw_password_label),
                    text = password,
                    onTextChange = { changedPassword ->
                        onEvent(WithdrawNoticeScreenEvent.PasswordChanged(changedPassword))
                    },
                    placeholder = stringResource(R.string.withdraw_password_placeholder),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = uiState.submissionState == WithdrawSubmissionState.InvalidPassword,
                    supportingText = if (uiState.submissionState == WithdrawSubmissionState.InvalidPassword) {
                        stringResource(R.string.auth_password_error)
                    } else {
                        null
                    },
                )

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

                WithdrawAgreementRow(
                    agreementText = stringResource(R.string.withdraw_agree),
                    isAgreed = uiState.isAgreed,
                    onAgreementChange = { isAgreed ->
                        onEvent(WithdrawNoticeScreenEvent.AgreementChanged(isAgreed))
                    },
                )
            }

            if (uiState.submissionState == WithdrawSubmissionState.Failed) {
                Text(
                    text = stringResource(R.string.withdraw_failure_message),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.BottomActionHorizontal),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            WithdrawDangerButton(
                title = stringResource(R.string.withdraw_action),
                onClick = { onEvent(WithdrawNoticeScreenEvent.WithdrawClicked) },
                isEnabled = uiState.canSubmit,
                modifier = Modifier.padding(
                    horizontal = HeartGuardSpacing.BottomActionHorizontal,
                    vertical = HeartGuardSpacing.Section,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun WithdrawNoticeScreenPreview() {
    HeartGuardTheme {
        WithdrawNoticeScreen(
            uiState = WithdrawUiState(
                selectedReason = WithdrawReason.FIELD_WORK_ENDED,
            ),
            password = "",
            onEvent = {},
        )
    }
}
