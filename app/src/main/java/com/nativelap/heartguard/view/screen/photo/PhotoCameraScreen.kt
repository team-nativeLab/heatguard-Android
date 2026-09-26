package com.nativelap.heartguard.view.screen.photo

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** CameraX 미리보기 위에 촬영 조작과 권한·오류 안내를 표시한다. */
@Composable
internal fun PhotoCameraScreen(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    title: String,
    hasCameraPermission: Boolean,
    isCameraReady: Boolean,
    isCapturing: Boolean,
    isPermissionDenied: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCaptureClick: () -> Unit,
) {
    val captureContentDescription = stringResource(R.string.photo_camera_capture)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.inverseSurface),
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(176.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.78f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = HeartGuardSpacing.ScreenHorizontal),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.photo_camera_close),
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }

                Text(
                    text = title,
                    modifier = Modifier.padding(start = HeartGuardSpacing.Compact),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!hasCameraPermission || !isCameraReady || errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = HeartGuardSpacing.LargeSection),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                ) {
                    Text(
                        text = errorMessage ?: stringResource(R.string.photo_camera_starting),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    if (isPermissionDenied) {
                        Button(onClick = onRequestPermissionClick) {
                            Text(text = stringResource(R.string.photo_camera_request_permission))
                        }
                        Button(onClick = onOpenSettingsClick) {
                            Text(text = stringResource(R.string.photo_camera_open_settings))
                        }
                    } else if (hasCameraPermission && !isCameraReady && errorMessage != null) {
                        Button(onClick = onRetryClick) {
                            Text(text = stringResource(R.string.photo_camera_retry))
                        }
                    } else if (!hasCameraPermission) {
                        Button(onClick = onRequestPermissionClick) {
                            Text(text = stringResource(R.string.photo_camera_request_permission))
                        }
                    } else if (!isCameraReady) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = HeartGuardSpacing.LargeSection),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .size(HeartGuardComponentSize.CameraAction + 8.dp)
                        .clickable(
                            enabled = hasCameraPermission && isCameraReady && !isCapturing,
                            onClickLabel = captureContentDescription,
                            onClick = onCaptureClick,
                        ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(HeartGuardComponentSize.CameraAction),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(HeartGuardComponentSize.CameraAction),
                            )
                        }
                    }
                }
            }
        }
    }
}
