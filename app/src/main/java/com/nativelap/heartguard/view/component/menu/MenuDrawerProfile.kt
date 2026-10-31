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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.valueOrEmptyText
import com.nativelap.heartguard.viewmodel.menu.isHttpsProfilePhotoUrl
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerProfileUiModel

/** 메뉴 드로어 상단에 프로필 사진 또는 이름 첫 글자 아바타와 사용자 정보를 보여준다. */
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
            modifier =
                Modifier
                    .size(HeartGuardIconSize.MenuAvatar)
                    .background(
                        color = MaterialTheme.extraColors.photoContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            val photoUrl = profile.photoUrl.takeIf(::isHttpsProfilePhotoUrl)
            if (photoUrl != null) {
                SubcomposeAsyncImage(
                    model =
                        ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                    contentDescription = stringResource(R.string.menu_profile_photo_description),
                    modifier = Modifier.size(HeartGuardIconSize.MenuAvatar).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = { MenuDrawerAvatarInitial(profile.avatarInitial) },
                    error = { MenuDrawerAvatarInitial(profile.avatarInitial) },
                    success = { SubcomposeAsyncImageContent() },
                )
            } else {
                MenuDrawerAvatarInitial(profile.avatarInitial)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = valueOrEmptyText(profile.userName),
                color = MaterialTheme.extraColors.strongText,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = HeartGuardFontSize.MenuProfileName,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.menu_profile_affiliation_format,
                        valueOrEmptyText(profile.companyName),
                        stringResource(R.string.profile_worker_role),
                    ),
                color = MaterialTheme.extraColors.secondaryText,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = HeartGuardFontSize.SmallLabel,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valueOrEmptyText(profile.email),
                color = MaterialTheme.extraColors.tertiaryText,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = HeartGuardFontSize.MenuProfileEmail,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MenuDrawerAvatarInitial(initial: String) {
    Text(
        text = valueOrEmptyText(initial),
        color = MaterialTheme.colorScheme.primary,
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontSize = HeartGuardFontSize.MenuProfileName,
                fontWeight = FontWeight.Bold,
            ),
    )
}

@Preview(showBackground = true, widthDp = 252)
@Composable
private fun MenuDrawerProfilePreview() {
    HeartGuardTheme {
        MenuDrawerProfile(
            profile =
                MenuDrawerProfileUiModel(
                    userName = "김현장",
                    companyName = "이음산업건설",
                    email = "worker@ieum.co.kr",
                ),
        )
    }
}
