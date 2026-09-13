package com.nativelap.heartguard.view.screen.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.emergency.EmergencyAlertBanner
import com.nativelap.heartguard.view.component.emergency.EmergencyCallButton
import com.nativelap.heartguard.view.component.emergency.EmergencyCallIndicator
import com.nativelap.heartguard.view.component.emergency.EmergencyContactCard

/** 긴급상황 안내와 관리자 연락 동작을 하나의 정적 화면으로 구성한다. */
@Composable
fun EmergencyScreen(
    contactName: String,
    phoneNumber: String,
    onCallClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(HeartGuardSpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            EmergencyAlertBanner(
                title = stringResource(R.string.emergency_alert_title),
                description = stringResource(R.string.emergency_alert_description),
            )

            EmergencyCallIndicator(
                title = stringResource(R.string.emergency_indicator_title),
                description = stringResource(R.string.emergency_indicator_description),
                isCalling = false,
            )

            EmergencyContactCard(
                contactTitle = stringResource(R.string.emergency_contact_title),
                contactName = contactName,
                phoneNumber = phoneNumber,
                onCallClick = onContactClick,
            )

            EmergencyCallButton(
                title = stringResource(R.string.emergency_call),
                onClick = onCallClick,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun EmergencyScreenPreview() {
    HeartGuardTheme {
        EmergencyScreen(
            contactName = "홍길동",
            phoneNumber = "010-1234-5678",
            onCallClick = {},
            onContactClick = {},
        )
    }
}
