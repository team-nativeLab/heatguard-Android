package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

@Composable
fun HomeEmergencyPhoneCallDialog(
    phoneNumber: String,
    onDismissRequest: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .widthIn(max = HeartGuardComponentSize.DialogMaxWidth)
                .fillMaxWidth()
                .testTag(HOME_EMERGENCY_CALL_DIALOG_TEST_TAG),
        shape = RoundedCornerShape(HeartGuardRadius.Dialog),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            Text(
                text = stringResource(R.string.home_headquarters_contact_confirmation),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(HOME_EMERGENCY_CALL_DIALOG_TEXT_TEST_TAG),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = phoneNumber,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(HOME_EMERGENCY_CALL_PHONE_NUMBER_TEST_TAG),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        HeartGuardSpacing.Compact,
                        Alignment.End,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.home_emergency_call_dismiss))
                }
                TextButton(onClick = onCallClick) {
                    Text(text = stringResource(R.string.home_emergency_call_confirm))
                }
            }
        }
    }
}

private const val HOME_EMERGENCY_CALL_DIALOG_TEST_TAG = "homeEmergencyCallConfirmation"
private const val HOME_EMERGENCY_CALL_DIALOG_TEXT_TEST_TAG = "homeEmergencyCallConfirmationText"
private const val HOME_EMERGENCY_CALL_PHONE_NUMBER_TEST_TAG = "homeEmergencyCallPhoneNumber"
