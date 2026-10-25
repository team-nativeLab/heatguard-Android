package com.nativelap.heartguard.view.component.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardComponentSize.ProfileAvatar)
                    .background(
                        color = MaterialTheme.extraColors.photoContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    userName
                        ?.firstOrNull()
                        ?.toString()
                        ?: stringResource(R.string.common_empty_value),
                color = MaterialTheme.colorScheme.primary,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = HeartGuardFontSize.ProfileAvatar,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

        Text(
            text = stringResource(R.string.profile_worker_role),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ProfileAvatarHeaderPreview() {
    HeartGuardTheme {
        ProfileAvatarHeader(userName = "김현장")
    }
}
