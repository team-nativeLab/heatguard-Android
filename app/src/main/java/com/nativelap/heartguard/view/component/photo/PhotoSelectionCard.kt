package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 작업·휴식 사진 화면의 큰 사진 선택 영역과 촬영 안내를 담당한다. */
@Composable
fun PhotoSelectionCard(
    title: String,
    description: String,
    cameraPainter: Painter,
    cameraContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedPhotoCountLabel: String? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HeartGuardComponentSize.PhotoSelectionHeight)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        color = MaterialTheme.extraColors.photoContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.LargeSection),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = cameraPainter,
                contentDescription = cameraContentDescription,
                modifier = Modifier.size(HeartGuardIconSize.CameraAction),
            )
            Text(
                text = title,
                modifier = Modifier.padding(top = HeartGuardSpacing.Item),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            selectedPhotoCountLabel?.let { countLabel ->
                Text(
                    text = countLabel,
                    modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PhotoSelectionCardPreview() {
    HeartGuardTheme {
        PhotoSelectionCard(
            title = "사진을 촬영해 주세요",
            description = "작업 현장이 잘 보이도록 촬영해 주세요",
            cameraPainter = androidx.compose.ui.res.painterResource(R.drawable.record_camera),
            cameraContentDescription = "사진 촬영",
            onClick = {},
            selectedPhotoCountLabel = "0/2",
        )
    }
}
