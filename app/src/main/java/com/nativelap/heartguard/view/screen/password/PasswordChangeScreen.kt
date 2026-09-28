package com.nativelap.heartguard.view.screen.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.password.PasswordGuideState
import com.nativelap.heartguard.view.component.password.PasswordGuideText
import com.nativelap.heartguard.view.component.password.PasswordInputField
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.password.PasswordChangeError
import com.nativelap.heartguard.viewmodel.password.PasswordChangeScreenEvent
import com.nativelap.heartguard.viewmodel.password.PasswordChangeUiState

/** Figma 26_비밀번호변경(기본)·27(확인 불일치)·28(입력 완료) 화면이다. 입력 규칙과 확인 일치를 즉시 안내하고,
 * 모두 충족할 때만 "변경하기"를 활성화한다. 서버 오류(현재 비밀번호 불일치 등)는 해당 입력칸 아래에 표시한다. */
@Composable
fun PasswordChangeScreen(
    uiState: PasswordChangeUiState,
    onEvent: (PasswordChangeScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.password_change_submit),
                    onClick = { onEvent(PasswordChangeScreenEvent.SubmitClicked) },
                    enabled = uiState.canSubmit,
                )
            }
        },
    ) { innerPadding ->
        ResponsivePageContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.password_change_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(PasswordChangeScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                    .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
                Text(
                    text = stringResource(R.string.password_change_description),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Column {
                    PasswordInputField(
                        label = stringResource(R.string.password_change_current_label),
                        password = uiState.currentPassword,
                        placeholder = stringResource(R.string.password_change_current_placeholder),
                        isPasswordVisible = uiState.isCurrentPasswordVisible,
                        onPasswordChange = { changedPassword ->
                            onEvent(PasswordChangeScreenEvent.CurrentPasswordChanged(changedPassword))
                        },
                        onVisibilityClick = { onEvent(PasswordChangeScreenEvent.CurrentPasswordVisibilityClicked) },
                        isError = uiState.submitError == PasswordChangeError.INVALID_CURRENT_PASSWORD,
                    )
                    if (uiState.submitError == PasswordChangeError.INVALID_CURRENT_PASSWORD) {
                        PasswordGuideText(
                            guideText = stringResource(R.string.password_change_invalid_current),
                            guideState = PasswordGuideState.ERROR,
                        )
                    }
                }

                Column {
                    PasswordInputField(
                        label = stringResource(R.string.password_change_new_label),
                        password = uiState.newPassword,
                        placeholder = stringResource(R.string.password_change_new_placeholder),
                        isPasswordVisible = uiState.isNewPasswordVisible,
                        onPasswordChange = { changedPassword ->
                            onEvent(PasswordChangeScreenEvent.NewPasswordChanged(changedPassword))
                        },
                        onVisibilityClick = { onEvent(PasswordChangeScreenEvent.NewPasswordVisibilityClicked) },
                        isError = uiState.submitError == PasswordChangeError.INVALID_NEW_PASSWORD,
                    )
                    PasswordGuideText(
                        guideText = when {
                            uiState.submitError == PasswordChangeError.INVALID_NEW_PASSWORD -> {
                                stringResource(R.string.password_change_invalid_new)
                            }

                            uiState.isNewPasswordValid -> stringResource(R.string.password_change_rule_satisfied)
                            else -> stringResource(R.string.password_change_rule)
                        },
                        guideState = when {
                            uiState.submitError == PasswordChangeError.INVALID_NEW_PASSWORD -> PasswordGuideState.ERROR
                            uiState.isNewPasswordValid -> PasswordGuideState.SATISFIED
                            else -> PasswordGuideState.NEUTRAL
                        },
                    )
                }

                Column {
                    PasswordInputField(
                        label = stringResource(R.string.password_change_confirm_label),
                        password = uiState.confirmPassword,
                        placeholder = stringResource(R.string.password_change_confirm_placeholder),
                        isPasswordVisible = uiState.isConfirmPasswordVisible,
                        onPasswordChange = { changedPassword ->
                            onEvent(PasswordChangeScreenEvent.ConfirmPasswordChanged(changedPassword))
                        },
                        onVisibilityClick = { onEvent(PasswordChangeScreenEvent.ConfirmPasswordVisibilityClicked) },
                        isError = uiState.isConfirmMismatched,
                        imeAction = ImeAction.Done,
                    )
                    if (uiState.isConfirmMismatched) {
                        PasswordGuideText(
                            guideText = stringResource(R.string.password_change_confirm_mismatch),
                            guideState = PasswordGuideState.ERROR,
                        )
                    } else if (uiState.isConfirmMatched) {
                        PasswordGuideText(
                            guideText = stringResource(R.string.password_change_confirm_match),
                            guideState = PasswordGuideState.SATISFIED,
                        )
                    }
                }

                if (uiState.submitError == PasswordChangeError.FAILURE) {
                    PasswordGuideText(
                        guideText = stringResource(R.string.password_change_failure),
                        guideState = PasswordGuideState.ERROR,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun PasswordChangeScreenMismatchPreview() {
    HeartGuardTheme {
        PasswordChangeScreen(
            uiState = PasswordChangeUiState(
                currentPassword = "current1",
                newPassword = "abcd1234",
                confirmPassword = "abcd1235",
            ),
            onEvent = {},
        )
    }
}
