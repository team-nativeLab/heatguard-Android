package com.nativelap.heartguard.view.route.photo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.createPhotoCaptureTarget
import com.nativelap.heartguard.core.util.deleteCaptureFile
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.screen.photo.PhotoCameraScreen
import com.nativelap.heartguard.view.screen.photo.PhotoCameraScreenEvent
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

@Composable
internal fun HeartGuardPhotoCameraRoute(
    recordDraftViewModel: RecordDraftViewModel,
    recordType: RecordType,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val pendingCaptureUriState =
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    var pendingCaptureUriString by pendingCaptureUriState
    var errorMessageResourceId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }
    var hasRequestedInitialCapture by rememberSaveable {
        mutableStateOf(false)
    }

    val captureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { wasCaptureSuccessful ->
            val captureUri = pendingCaptureUriString?.let(Uri::parse)
            pendingCaptureUriString = null

            if (captureUri == null) {
                return@rememberLauncherForActivityResult
            }

            if (!wasCaptureSuccessful) {
                deleteCaptureFile(context, captureUri)
                onBackClick()
                return@rememberLauncherForActivityResult
            }

            if (!context.hasNonEmptyCapture(captureUri)) {
                deleteCaptureFile(context, captureUri)
                errorMessageResourceId = R.string.photo_camera_capture_failed
                return@rememberLauncherForActivityResult
            }

            if (recordDraftViewModel.addPhoto(recordType, captureUri)) {
                errorMessageResourceId = null
                onBackClick()
            } else {
                deleteCaptureFile(context, captureUri)
                errorMessageResourceId = R.string.photo_camera_limit_reached
            }
        }

    val launchCamera = {
        if (pendingCaptureUriString == null) {
            val captureTarget = createPhotoCaptureTarget(context)
            if (captureTarget == null) {
                errorMessageResourceId = R.string.photo_camera_file_unavailable
            } else {
                pendingCaptureUriString = captureTarget.uri.toString()
                errorMessageResourceId = null
                try {
                    captureLauncher.launch(captureTarget.uri)
                } catch (_: Exception) {
                    deleteCaptureFile(context, captureTarget.uri)
                    pendingCaptureUriString = null
                    errorMessageResourceId = R.string.photo_camera_unavailable
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedInitialCapture) {
            hasRequestedInitialCapture = true
            launchCamera()
        }
    }

    DisposableEffect(context) {
        onDispose {
            if (context.findActivity()?.isChangingConfigurations != true) {
                pendingCaptureUriState.value
                    ?.let(Uri::parse)
                    ?.let { captureUri -> deleteCaptureFile(context, captureUri) }
            }
        }
    }

    val screenTitle =
        when (recordType) {
            RecordType.TEMPERATURE -> stringResource(R.string.photo_camera_field_title)
            RecordType.WORK -> stringResource(R.string.photo_camera_work_title)
            RecordType.REST -> stringResource(R.string.photo_camera_rest_title)
        }
    val errorMessage = errorMessageResourceId?.let { stringResource(it) }

    PhotoCameraScreen(
        title = screenTitle,
        errorMessage = errorMessage,
        isCapturePending = pendingCaptureUriString != null,
        onEvent = { event ->
            when (event) {
                PhotoCameraScreenEvent.BackClicked -> {
                    onBackClick()
                }

                PhotoCameraScreenEvent.OpenSystemCameraClicked -> {
                    launchCamera()
                }
            }
        },
    )
}

private fun Context.hasNonEmptyCapture(uri: Uri): Boolean =
    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.read() != -1
        } ?: false
    } catch (_: Exception) {
        false
    }

private fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext !is Activity && currentContext is ContextWrapper) {
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}
