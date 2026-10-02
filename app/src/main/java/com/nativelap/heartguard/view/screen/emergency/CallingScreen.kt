package com.nativelap.heartguard.view.screen.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.HeartGuardSheetSurface
import com.nativelap.heartguard.view.component.emergency.CallCancelButton
import com.nativelap.heartguard.view.component.emergency.CallEndButton
import com.nativelap.heartguard.view.component.emergency.EmergencyAlertBanner
import com.nativelap.heartguard.view.component.emergency.EmergencyCallIndicator
import com.nativelap.heartguard.view.component.emergency.EmergencyContactCard

/** 긴급 호출의 대기·연결 상태를 동일한 화면 구조에서 정적으로 보여준다. */
@Composable
fun CallingScreen(
    isConnected: Boolean,
    contactName: String,
    phoneNumber: String,
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    isContactCallEnabled: Boolean = true,
    isUpdatingStatus: Boolean = false,
    statusUpdateError: Boolean = false,
    isConnectionUnstable: Boolean = false,
) {
    // Figma 03·04는 아래에서 올라오는 시트다. 호출 상태 변경이 확인되기 전에는 닫기 시도를 소비한다.
    HeartGuardSheetSurface(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(
                        top = HeartGuardSpacing.LargeSection,
                        bottom = HeartGuardSpacing.LargeSection,
                    ),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
            ) {
            Text(
                text = stringResource(R.string.emergency_screen_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Tight),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = HeartGuardFontSize.PageTitle,
                    lineHeight = HeartGuardFontSize.PageTitle,
                ),
            )

            EmergencyAlertBanner(
                title = stringResource(R.string.emergency_alert_title),
                description = stringResource(R.string.emergency_alert_description),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
            )

            EmergencyCallIndicator(
                title = if (isConnected) {
                    stringResource(R.string.emergency_connected_status)
                } else {
                    stringResource(R.string.emergency_calling_indicator_title)
                },
                description = if (isConnected) {
                    stringResource(R.string.emergency_connected_status_description)
                } else {
                    stringResource(R.string.emergency_calling_indicator_description)
                },
                isCalling = false,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isConnectionUnstable) {
                Text(
                    text = stringResource(R.string.emergency_connection_unstable),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordContentHorizontal),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (statusUpdateError) {
                Text(
                    text = stringResource(R.string.emergency_status_update_error),
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordContentHorizontal),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            EmergencyContactCard(
                contactTitle = stringResource(R.string.emergency_contact_title),
                contactName = contactName,
                phoneNumber = phoneNumber,
                onCallClick = onContactClick,
                isCallEnabled = isContactCallEnabled,
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordPhotoCardHorizontal),
            )

            if (isConnected) {
                CallEndButton(
                    title = stringResource(R.string.emergency_call_end),
                    onClick = onEndClick,
                    enabled = !isUpdatingStatus,
                    modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordContentHorizontal),
                )
            } else {
                CallCancelButton(
                    title = stringResource(R.string.emergency_call_cancel),
                    onClick = onCancelClick,
                    enabled = !isUpdatingStatus,
                    modifier = Modifier
                        .padding(horizontal = HeartGuardSpacing.RecordContentHorizontal)
                        .padding(top = HeartGuardSpacing.RecordFieldInset),
                )
            }
            }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun CallingScreenPreview() {
    HeartGuardTheme {
        CallingScreen(
            isConnected = false,
            contactName = stringResource(R.string.emergency_contact_name),
            phoneNumber = "010-1234-5678",
            onCancelClick = {},
            onEndClick = {},
            onContactClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun CallingScreenConnectedPreview() {
    HeartGuardTheme {
        CallingScreen(
            isConnected = true,
            contactName = stringResource(R.string.emergency_contact_name),
            phoneNumber = "010-1234-5678",
            onCancelClick = {},
            onEndClick = {},
            onContactClick = {},
        )
    }
}
