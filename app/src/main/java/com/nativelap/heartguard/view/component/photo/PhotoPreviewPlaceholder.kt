package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** 현장 사진 화면에서 촬영 전 빈 상태와 온도계 미저장 상태를 공통으로 보여준다. */
@Composable
fun PhotoPreviewPlaceholder(
    message: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clickModifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(HeartGuardComponentSize.PhotoPreviewHeight)
                .then(clickModifier),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        color = MaterialTheme.extraColors.photoContainer,
    ) {
        if (message != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(HeartGuardSpacing.Section),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = HeartGuardSpacing.Item,
                        alignment = Alignment.CenterVertically,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(HeartGuardIconSize.PhotoError),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = message,
                    modifier =
                        Modifier.widthIn(
                            max = HeartGuardComponentSize.TemperatureErrorMessageMaxWidth,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        }
    }
}
