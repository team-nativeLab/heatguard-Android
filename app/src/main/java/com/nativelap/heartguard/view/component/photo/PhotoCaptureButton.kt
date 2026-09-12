package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 작업·휴식 사진 촬영 화면에서 카메라 동작만 담당하는 원형 버튼이다. */
@Composable
fun PhotoCaptureButton(
    cameraPainter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.size(HeartGuardComponentSize.CameraAction),
    ) {
        Box(
            modifier = Modifier
                .size(HeartGuardComponentSize.CameraAction)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = cameraPainter,
                contentDescription = contentDescription,
                modifier = Modifier.size(HeartGuardIconSize.CameraAction),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoCaptureButtonPreview() {
    HeartGuardTheme {
        PhotoCaptureButton(
            cameraPainter = androidx.compose.ui.res.painterResource(
                com.nativelap.heartguard.R.drawable.record_camera,
            ),
            contentDescription = "사진 촬영",
            onClick = {},
        )
    }
}
