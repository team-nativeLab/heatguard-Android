package com.nativelap.heartguard.view.screen.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.emergency.CallCancelButton
import com.nativelap.heartguard.view.component.emergency.EmergencyAlertBanner
import com.nativelap.heartguard.view.component.emergency.EmergencyCallIndicator
import com.nativelap.heartguard.view.component.emergency.EmergencyContactCard

/**
 * 긴급상황 안내와 관리자 연락 동작을 하나의 정적 화면으로 구성한다.
 * Figma "03_긴급상황" 화면이다. 큰 "긴급 호출하기" 버튼을 눌러야 긴급호출이 등록되고 04_호출중으로 이동한다
 * (화면에 들어오는 것만으로는 호출하지 않는다). "호출 취소"는 호출 전이므로 홈으로 돌아간다.
 */
@Composable
fun EmergencyScreen(
    contactName: String,
    phoneNumber: String,
    onCallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier,
    isContactCallEnabled: Boolean = true,
    isRegistering: Boolean = false,
    hasRegistrationFailed: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
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
                title = stringResource(R.string.emergency_indicator_title),
                description = stringResource(R.string.emergency_indicator_description),
                isCalling = true,
                // 등록 요청 중에는 중복 호출을 막기 위해 버튼을 누를 수 없게 한다.
                onClick = if (isRegistering) {
                    null
                } else {
                    onCallClick
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasRegistrationFailed) {
                Text(
                    text = stringResource(R.string.emergency_register_error),
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

            CallCancelButton(
                title = stringResource(R.string.emergency_call_cancel),
                onClick = onCancelClick,
                enabled = !isRegistering,
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
private fun EmergencyScreenPreview() {
    HeartGuardTheme {
        EmergencyScreen(
            contactName = "홍길동",
            phoneNumber = "010-1234-5678",
            onCancelClick = {},
            onCallClick = {},
            onContactClick = {},
        )
    }
}
