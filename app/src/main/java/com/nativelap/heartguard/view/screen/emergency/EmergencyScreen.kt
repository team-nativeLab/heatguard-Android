package com.nativelap.heartguard.view.screen.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.emergency.CallCancelButton
import com.nativelap.heartguard.view.component.emergency.EmergencyAlertBanner
import com.nativelap.heartguard.view.component.emergency.EmergencyCallIndicator
import com.nativelap.heartguard.view.component.emergency.EmergencyContactCard

/**
 * 긴급상황 안내와 관리자 연락 동작을 하나의 정적 화면으로 구성한다.
 * Figma "03_긴급상황" 프레임은 이미 호출이 대기 중인 경고 상태를 보여주며,
 * 별도의 "호출하기" primary 버튼 없이 [CallCancelButton]으로 호출을 취소하는 동작만 제공한다.
 */
@Composable
fun EmergencyScreen(
    contactName: String,
    phoneNumber: String,
    onCancelClick: () -> Unit,
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
            Text(
                text = stringResource(R.string.emergency_screen_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = HeartGuardFontSize.PageTitle,
                    lineHeight = HeartGuardFontSize.PageTitle,
                ),
            )

            EmergencyAlertBanner(
                title = stringResource(R.string.emergency_alert_title),
                description = stringResource(R.string.emergency_alert_description),
            )

            EmergencyCallIndicator(
                title = stringResource(R.string.emergency_indicator_title),
                description = stringResource(R.string.emergency_indicator_description),
                isCalling = true,
            )

            EmergencyContactCard(
                contactTitle = stringResource(R.string.emergency_contact_title),
                contactName = contactName,
                phoneNumber = phoneNumber,
                onCallClick = onContactClick,
            )

            CallCancelButton(
                title = stringResource(R.string.emergency_call_cancel),
                onClick = onCancelClick,
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
            onCancelClick = {},
            onContactClick = {},
        )
    }
}
