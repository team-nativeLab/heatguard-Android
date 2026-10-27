package com.nativelap.heartguard.view.component.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 내 정보 수정 화면 상단의 이름 첫 글자 아바타와 역할 라벨이다. 이름이 없으면 "--"를 보여준다. */
@Composable
fun ProfileAvatarHeader(
    userName: String?,
    modifier: Modifier = Modifier,
    photoUri: Uri? = null,
    photoUrl: String? = null,
    onPhotoClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    photoActionsMenuContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardComponentSize.ProfileAvatar)
                    .then(if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick) else Modifier)
                    .background(
                        color = MaterialTheme.extraColors.photoContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUri != null || !photoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = photoUri ?: photoUrl,
                    contentDescription = stringResource(R.string.profile_photo_preview),
                    modifier = Modifier.size(HeartGuardComponentSize.ProfileAvatar).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = { AvatarInitial(userName) },
                    error = { AvatarInitial(userName) },
                    success = { SubcomposeAsyncImageContent() },
                )
            } else {
                AvatarInitial(userName)
            }

            if (onAvatarClick != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(HeartGuardComponentSize.ProfilePhotoBadge)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.record_camera),
                        contentDescription = null,
                        modifier = Modifier.size(HeartGuardComponentSize.ProfilePhotoCameraIcon),
                    )
                }
            }

            photoActionsMenuContent?.invoke(this)
        }

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

        Text(
            text =
                stringResource(
                    if (onPhotoClick !=
                        null
                    ) {
                        R.string.profile_photo_select
                    } else {
                        R.string.profile_worker_role
                    },
                ),
            modifier = if (onPhotoClick != null) Modifier.clickable(onClick = onPhotoClick) else Modifier,
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AvatarInitial(userName: String?) {
    Text(
        text = userName?.firstOrNull()?.toString() ?: stringResource(R.string.common_empty_value),
        color = MaterialTheme.colorScheme.primary,
        style =
            MaterialTheme.typography.titleLarge.copy(
                fontSize = HeartGuardFontSize.ProfileAvatar,
                fontWeight = FontWeight.Bold,
            ),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileAvatarHeaderPreview() {
    HeartGuardTheme {
        ProfileAvatarHeader(userName = "김현장")
    }
}
