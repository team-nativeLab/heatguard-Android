package com.nativelap.heartguard.view.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
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
                    .padding(horizontal = HeartGuardSpacing.HeaderHorizontal)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(HeartGuardSpacing.AuthTop - innerPadding.calculateTopPadding()))
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
                    Box(
                        modifier =
                            Modifier.width(1.dp).height(HeartGuardSpacing.Compact).background(
                                MaterialTheme.extraColors.subtleDivider,
                            ),
                    )
                    Spacer(modifier = Modifier.width(HeartGuardSpacing.Compact))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { enteredPhoneNumber ->
                            onPhoneNumberChange(enteredPhoneNumber.filter { digit -> digit in '0'..'9' })
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.auth_phone_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = {
                            if (phoneNumber.isNotEmpty()) {
                                IconButton(onClick = { onPhoneNumberChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.common_clear),
                                        tint = MaterialTheme.extraColors.tertiaryText,
                                    )
                                }
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.extraColors.tertiaryText,
                    )
                    Text(
                        text = stringResource(R.string.auth_phone_security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun AuthPhoneNumberScreenPreview() {
    HeartGuardTheme {
        AuthPhoneNumberScreen("", {}, {})
    }
}
