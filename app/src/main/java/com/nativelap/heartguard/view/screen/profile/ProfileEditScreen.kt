package com.nativelap.heartguard.view.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.UnavailableFeatureCard
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.profile.ProfileEditScreenEvent

/** Figma 24_내정보수정 화면의 구조를 보여주며, 작업자 프로필 API가 없어 조회·수정은 비활성 상태다. */
@Composable
fun ProfileEditScreen(
    companyName: String,
    userName: String,
    email: String,
    onEvent: (ProfileEditScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val emptyValue = stringResource(R.string.common_empty_value)
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title = stringResource(R.string.profile_save),
                    onClick = {},
                    enabled = false,
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                    .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = HeartGuardSpacing.Tight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(HeartGuardComponentSize.ProfileAvatar)
                            .background(MaterialTheme.extraColors.photoContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (userName.isBlank() || userName == emptyValue) {
                                emptyValue
                            } else {
                                userName.firstOrNull()?.toString().orEmpty()
                            },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = HeartGuardFontSize.ProfileAvatar,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                    Text(
                        text = stringResource(R.string.profile_worker_role),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                UnavailableFeatureCard(
                    title = stringResource(R.string.profile_api_unavailable_title),
                    description = stringResource(R.string.profile_api_unavailable_description),
                )

                ProfileValueField(
                    label = stringResource(R.string.profile_company),
                    value = companyName,
                )
                ProfileValueField(
                    label = stringResource(R.string.profile_name),
                    value = userName,
                )
                ProfileValueField(
                    label = stringResource(R.string.profile_email),
                    value = email,
                    trailingText = stringResource(R.string.profile_email_uneditable),
                    supportingText = stringResource(R.string.profile_email_note),
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HeartGuardRadius.Card),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(HeartGuardSpacing.Item),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.profile_password_change),
                                color = MaterialTheme.extraColors.strongText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Spacer(modifier = Modifier.height(HeartGuardSpacing.Tight))
                            Text(
                                text = stringResource(R.string.profile_password_api_unavailable),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = stringResource(R.string.common_chevron_right),
                            color = MaterialTheme.extraColors.tertiaryText,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileValueField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    supportingText: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HeartGuardSpacing.Compact),
            shape = RoundedCornerShape(HeartGuardRadius.InputBox),
            color = MaterialTheme.extraColors.authInputBackground,
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .heightIn(min = HeartGuardComponentSize.TextFieldHeight)
                    .padding(horizontal = HeartGuardSpacing.Item),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value.ifBlank { stringResource(R.string.common_empty_value) },
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        color = MaterialTheme.extraColors.tertiaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = HeartGuardSpacing.Tight),
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ProfileEditScreenUnavailablePreview() {
    HeartGuardTheme {
        ProfileEditScreen(
            companyName = "--",
            userName = "--",
            email = "--",
            onEvent = {},
        )
    }
}
