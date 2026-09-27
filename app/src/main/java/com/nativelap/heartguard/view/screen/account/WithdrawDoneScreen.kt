package com.nativelap.heartguard.view.screen.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.account.WithdrawDoneMessage
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton

/** Figma 19_회원탈퇴_완료 화면이다. 이미 탈퇴가 끝났으므로 뒤로 가기 버튼 없이 확인 버튼만 둔다. */
@Composable
fun WithdrawDoneScreen(
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.withdraw_title),
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            Spacer(modifier = Modifier.height(HeartGuardSpacing.Section))

            WithdrawDoneMessage(
                title = stringResource(R.string.withdraw_done_title),
                description = stringResource(R.string.withdraw_done_message),
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.ResultHorizontal),
            )

            Spacer(modifier = Modifier.weight(1f))

            RecordSaveButton(
                title = stringResource(R.string.common_confirm),
                onClick = onConfirmClick,
                modifier = Modifier.padding(
                    horizontal = HeartGuardSpacing.BottomActionHorizontal,
                    vertical = HeartGuardSpacing.Section,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 683)
@Composable
private fun WithdrawDoneScreenPreview() {
    HeartGuardTheme {
        WithdrawDoneScreen(onConfirmClick = {})
    }
}
