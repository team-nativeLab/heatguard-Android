package com.nativelap.heartguard.view.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.LoadErrorCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.auth.AuthTextField
import com.nativelap.heartguard.view.component.profile.ProfileAvatarHeader
import com.nativelap.heartguard.view.component.profile.ProfilePasswordChangeRow
import com.nativelap.heartguard.view.component.profile.ProfileReadOnlyField
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.profile.ProfileEditScreenEvent
import com.nativelap.heartguard.viewmodel.profile.ProfileEditUiState
import com.nativelap.heartguard.viewmodel.profile.ProfileLoadState
import com.nativelap.heartguard.viewmodel.profile.ProfileSaveError

/** Figma 24_내정보수정 화면이다. 이름·이메일은 작업자 정보 조회(GET /auth/team/me) 값이고 이름만 수정할 수 있다.
 * 회사명은 서버가 제공하지 않아 "--"로 표시한다. "비밀번호 변경" 행은 비밀번호 변경 화면(Figma 26)으로 이동한다. */
@Composable
fun ProfileEditScreen(
    uiState: ProfileEditUiState,
    onEvent: (ProfileEditScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadState = uiState.loadState
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.profile_save),
                    onClick = { onEvent(ProfileEditScreenEvent.SaveClicked) },
                    enabled = uiState.canSave,
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
                title = stringResource(R.string.profile_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(ProfileEditScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            val loadedProfile = loadState as? ProfileLoadState.Loaded
            if (loadState == ProfileLoadState.Loading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                        .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                ) {
                    ProfileAvatarHeader(
                        userName = loadedProfile?.userName,
                        modifier = Modifier.padding(bottom = HeartGuardSpacing.Tight),
                    )

                    if (loadState == ProfileLoadState.Failed) {
                        LoadErrorCard(
                            title = stringResource(R.string.profile_load_failure_title),
                            description = stringResource(R.string.profile_load_failure_description),
                            onRetryClick = { onEvent(ProfileEditScreenEvent.RetryClicked) },
                        )
                    }

                    ProfileReadOnlyField(
                        label = stringResource(R.string.profile_company),
                        value = loadedProfile?.companyName,
                    )

                    if (loadedProfile != null) {
                        AuthTextField(
                            label = stringResource(R.string.profile_name),
                            text = uiState.nameInput,
                            onTextChange = { changedName ->
                                onEvent(ProfileEditScreenEvent.NameChanged(changedName))
                            },
                            placeholder = stringResource(R.string.profile_name_placeholder),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            isError = uiState.saveError != null,
                            supportingText = when (uiState.saveError) {
                                ProfileSaveError.FAILURE -> stringResource(R.string.profile_save_failure)
                                ProfileSaveError.CONFLICT -> stringResource(R.string.profile_save_conflict)
                                null -> null
                            },
                        )
                    } else {
                        ProfileReadOnlyField(
                            label = stringResource(R.string.profile_name),
                            value = null,
                        )
                    }

                    ProfileReadOnlyField(
                        label = stringResource(R.string.profile_email),
                        value = loadedProfile?.email,
                        trailingText = stringResource(R.string.profile_email_uneditable),
                        supportingText = stringResource(R.string.profile_email_note),
                    )

                    ProfilePasswordChangeRow(
                        onClick = { onEvent(ProfileEditScreenEvent.PasswordChangeClicked) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ProfileEditScreenLoadedPreview() {
    HeartGuardTheme {
        ProfileEditScreen(
            uiState = ProfileEditUiState(
                loadState = ProfileLoadState.Loaded(
                    userName = "김현장",
                    email = "worker01",
                    companyName = "이음산업건설",
                ),
                nameInput = "김현장",
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ProfileEditScreenFailedPreview() {
    HeartGuardTheme {
        ProfileEditScreen(
            uiState = ProfileEditUiState(
                loadState = ProfileLoadState.Failed,
            ),
            onEvent = {},
        )
    }
}
