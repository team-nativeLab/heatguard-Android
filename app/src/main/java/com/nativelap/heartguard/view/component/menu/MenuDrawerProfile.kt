package com.nativelap.heartguard.view.component.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerProfileUiModel

/** 메뉴 드로어 상단에 이름 첫 글자 아바타와 이름·소속·이메일을 보여준다. */
@Composable
fun MenuDrawerProfile(
    profile: MenuDrawerProfileUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
        Box(
            modifier = Modifier
                .size(HeartGuardIconSize.MenuAvatar)
                .background(
                    color = MaterialTheme.extraColors.photoContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.avatarInitial,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = HeartGuardFontSize.MenuProfileName,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.userName,
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = HeartGuardFontSize.MenuProfileName,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.menu_profile_affiliation_format,
                    profile.companyName,
                    profile.jobTitle,
                ),
                color = MaterialTheme.extraColors.secondaryText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = HeartGuardFontSize.SmallLabel,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = profile.email,
                color = MaterialTheme.extraColors.tertiaryText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = HeartGuardFontSize.MenuProfileEmail,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 252)
@Composable
private fun MenuDrawerProfilePreview() {
    HeartGuardTheme {
        MenuDrawerProfile(
            profile = MenuDrawerProfileUiModel(
                userName = "김현장",
                companyName = "이음산업건설",
                jobTitle = "현장작업자",
                email = "worker@ieum.co.kr",
            ),
        )
    }
}
