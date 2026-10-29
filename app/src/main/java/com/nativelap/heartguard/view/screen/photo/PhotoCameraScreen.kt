package com.nativelap.heartguard.view.screen.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

internal sealed interface PhotoCameraScreenEvent {
    data object BackClicked : PhotoCameraScreenEvent

    data object OpenSystemCameraClicked : PhotoCameraScreenEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoCameraScreen(
    title: String,
    errorMessage: String?,
    isCapturePending: Boolean,
    onEvent: (PhotoCameraScreenEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(PhotoCameraScreenEvent.BackClicked) },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.photo_camera_close),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = HeartGuardSpacing.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    HeartGuardSpacing.Section,
                    Alignment.CenterVertically,
                ),
        ) {
            Text(
                text = stringResource(R.string.photo_camera_system_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCapturePending,
                onClick = { onEvent(PhotoCameraScreenEvent.OpenSystemCameraClicked) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.photo_camera_open_system_camera),
                )
            }
        }
    }
}
