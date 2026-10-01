package com.nativelap.heartguard.view.component.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * 높이가 충분하면 종료 동작을 아래에 두고, 작은 창에서는 전체 내용을 스크롤한다. */
@Composable
fun MenuDrawerContent(
    profile: MenuDrawerProfileUiModel,
    onEvent: (MenuDrawerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val availableHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = availableHeight),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
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
                    title = stringResource(R.string.menu_inquiry),
                    onClick = { onEvent(MenuDrawerEvent.InquiryClicked) },
                )
            }

            Column {
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
                email = "worker@ieum.co.kr",
            ),
            onEvent = {},
        )
    }
}
