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
import com.nativelap.heartguard.view.component.emergency.CallEndButton
import com.nativelap.heartguard.view.component.emergency.CallStatusIndicator
import com.nativelap.heartguard.view.component.emergency.EmergencyCallIndicator

/** 긴급 호출의 대기·연결 상태를 동일한 화면 구조에서 정적으로 보여준다. */
@Composable
fun CallingScreen(
    isConnected: Boolean,
    onCancelClick: () -> Unit,
    onEndClick: () -> Unit,
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
            // TODO: strings.xml에 emergency_screen_title(값: "긴급 호출") 키 추가 필요 — 지금은 하드코딩한다.
            Text(
                text = "긴급 호출",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = HeartGuardFontSize.PageTitle,
                    lineHeight = HeartGuardFontSize.PageTitle,
                ),
            )

            EmergencyCallIndicator(
                title = if (isConnected) {
                    stringResource(R.string.emergency_connected_status)
                } else {
                    stringResource(R.string.emergency_calling_status)
                },
                description = if (isConnected) {
                    stringResource(R.string.emergency_connected_status_description)
                } else {
                    stringResource(R.string.emergency_calling_description)
                },
                isCalling = isConnected,
            )

            CallStatusIndicator(
                statusTitle = if (isConnected) {
                    stringResource(R.string.emergency_connected)
                } else {
                    stringResource(R.string.emergency_calling)
                },
                statusDescription = if (isConnected) {
                    stringResource(R.string.emergency_connected_status_description)
                } else {
                    stringResource(R.string.emergency_waiting_status_description)
                },
                isConnected = isConnected,
            )

            if (isConnected) {
                CallEndButton(
                    title = stringResource(R.string.emergency_call_end),
                    onClick = onEndClick,
                )
            } else {
                CallCancelButton(
                    title = stringResource(R.string.emergency_call_cancel),
                    onClick = onCancelClick,
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
            onCancelClick = {},
            onEndClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun CallingScreenConnectedPreview() {
    HeartGuardTheme {
        CallingScreen(
            isConnected = true,
            onCancelClick = {},
            onEndClick = {},
        )
    }
}
