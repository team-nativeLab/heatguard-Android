package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.auth.AuthPrimaryButton
import com.nativelap.heartguard.view.component.auth.AuthTitleBlock

@Composable
internal fun AuthPhoneNumberScreen(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onLinkClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        topBar = {
            TextButton(
                modifier = Modifier.statusBarsPadding(),
                onClick = onBackClick,
            ) {
                Text(stringResource(R.string.common_back_description))
            }
        },
        bottomBar = {
            BottomActionBar {
                AuthPrimaryButton(
                    title = stringResource(R.string.auth_phone_link),
                    onClick = onLinkClick,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = HeartGuardSpacing.AuthHorizontal)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTitleForm))
                AuthTitleBlock(
                    title = stringResource(R.string.auth_phone_title),
                    description = stringResource(R.string.auth_phone_description),
                )
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTitleForm))
                Text(
                    text = stringResource(R.string.auth_phone_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthFieldLabelInput))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_phone_country_code),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.width(HeartGuardSpacing.Compact))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.auth_phone_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = {
                            if (phoneNumber.isNotEmpty()) {
                                TextButton(onClick = { onPhoneNumberChange("") }) {
                                    Text(stringResource(R.string.common_clear))
                                }
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                Text(
                    text = stringResource(R.string.auth_phone_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthPhoneNumberScreenPreview() {
    HeartGuardTheme {
        AuthPhoneNumberScreen("", {}, {}, {})
    }
}
