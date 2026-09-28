package com.nativelap.heartguard.view.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import kotlinx.coroutines.launch

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val profileUnavailableMessage = stringResource(R.string.profile_unavailable_message)
    val showUnavailableMessage: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = profileUnavailableMessage)
        }
        Unit
    }

    Scaffold(
        modifier = modifier.heartGuardResponsivePage(MaterialTheme.extraColors.pageBackground),
        containerColor = MaterialTheme.extraColors.pageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = HeartGuardSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.extraColors.pageBackground,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "‹",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.profile_edit_title),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        bottomBar = {
            Surface(
                onClick = showUnavailableMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HeartGuardSpacing.Section,
                        vertical = HeartGuardSpacing.Compact,
                    )
                    .height(48.dp),
                shape = RoundedCornerShape(HeartGuardRadius.Button),
                color = MaterialTheme.extraColors.disabledContent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.profile_save_action),
                        color = MaterialTheme.extraColors.disabledText,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HeartGuardSpacing.Section)
                .padding(top = HeartGuardSpacing.Section, bottom = HeartGuardSpacing.LargeSection),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.extraColors.photoContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "?",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.profile_data_unavailable),
                    color = MaterialTheme.extraColors.homeMutedText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }

            ProfileReadOnlyField(
                label = stringResource(R.string.profile_company_label),
                value = stringResource(R.string.profile_data_unavailable),
            )
            ProfileReadOnlyField(
                label = stringResource(R.string.profile_name_label),
                value = stringResource(R.string.profile_data_unavailable),
            )
            Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
                ProfileReadOnlyField(
                    label = stringResource(R.string.profile_email_label),
                    value = stringResource(R.string.profile_email_unavailable),
                    isDisabled = true,
                )
                Text(
                    text = stringResource(R.string.profile_email_unavailable_description),
                    color = MaterialTheme.extraColors.homeMutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Surface(
                onClick = showUnavailableMessage,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HeartGuardRadius.Card),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(HeartGuardSpacing.Item),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_password_change),
                            color = MaterialTheme.extraColors.disabledText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = stringResource(R.string.profile_password_unavailable),
                            color = MaterialTheme.extraColors.homeMutedText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(R.string.common_chevron_right),
                        color = MaterialTheme.extraColors.disabledContent,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyField(
    label: String,
    value: String,
    isDisabled: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isDisabled) {
                MaterialTheme.extraColors.disabledContent
            } else {
                MaterialTheme.extraColors.authInputBackground
            },
        ) {
            Box(
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.Item),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = value,
                    color = if (isDisabled) {
                        MaterialTheme.extraColors.disabledText
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun ProfileEditScreenPreview() {
    HeartGuardTheme {
        ProfileEditScreen(onBackClick = {})
    }
}

@Preview(name = "Compact large text", showBackground = true, widthDp = 320, heightDp = 740, fontScale = 2f)
@Composable
private fun ProfileEditScreenCompactPreview() {
    HeartGuardTheme {
        ProfileEditScreen(onBackClick = {})
    }
}

@Preview(name = "Wide", showBackground = true, widthDp = 840, heightDp = 1024)
@Composable
private fun ProfileEditScreenWidePreview() {
    HeartGuardTheme {
        ProfileEditScreen(onBackClick = {})
    }
}
