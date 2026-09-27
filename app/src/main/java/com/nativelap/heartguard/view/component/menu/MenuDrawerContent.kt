package com.nativelap.heartguard.view.component.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerEvent
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerProfileUiModel

/** 드로어 패널 안의 프로필·메뉴 목록·로그아웃·회원탈퇴 영역을 Figma 순서대로 묶는다.
 * 목록과 하단 영역 사이는 남는 높이로 채워 로그아웃·회원탈퇴가 항상 패널 아래쪽에 붙게 한다. */
@Composable
fun MenuDrawerContent(
    profile: MenuDrawerProfileUiModel,
    onEvent: (MenuDrawerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        MenuDrawerProfile(
            profile = profile,
            modifier = Modifier.padding(bottom = HeartGuardSpacing.LargeSection),
        )

        HorizontalDivider(
            thickness = HeartGuardBorderWidth.Divider,
            color = MaterialTheme.extraColors.subtleDivider,
        )

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

        MenuDrawerItem(
            title = stringResource(R.string.menu_edit_profile),
            onClick = { onEvent(MenuDrawerEvent.EditProfileClicked) },
        )
        MenuDrawerItem(
            title = stringResource(R.string.menu_notification_settings),
            onClick = { onEvent(MenuDrawerEvent.NotificationSettingsClicked) },
        )
        MenuDrawerItem(
            title = stringResource(R.string.menu_notices),
            onClick = { onEvent(MenuDrawerEvent.NoticesClicked) },
        )
        MenuDrawerItem(
            title = stringResource(R.string.menu_customer_center),
            onClick = { onEvent(MenuDrawerEvent.CustomerCenterClicked) },
        )

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            thickness = HeartGuardBorderWidth.Divider,
            color = MaterialTheme.extraColors.subtleDivider,
        )

        MenuDrawerItem(
            title = stringResource(R.string.menu_logout),
            onClick = { onEvent(MenuDrawerEvent.LogoutClicked) },
            showsChevron = false,
        )
        MenuDrawerWithdrawLink(
            title = stringResource(R.string.menu_withdraw),
            onClick = { onEvent(MenuDrawerEvent.WithdrawClicked) },
        )
    }
}

@Preview(showBackground = true, widthDp = 252, heightDp = 780)
@Composable
private fun MenuDrawerContentPreview() {
    HeartGuardTheme {
        MenuDrawerContent(
            profile = MenuDrawerProfileUiModel(
                userName = "김현장",
                companyName = "이음산업건설",
                jobTitle = "현장작업자",
                email = "worker@ieum.co.kr",
            ),
            onEvent = {},
        )
    }
}
