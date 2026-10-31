package com.nativelap.heartguard.view.route.photo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import com.nativelap.heartguard.core.util.releasePhoto
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.view.component.photo.PhotoSourceBottomSheet

/** 사진 유형별 ViewModel 목록에 앨범 사진을 추가하고 카메라 화면 진입을 제공한다. */
@Composable
internal fun PhotoSelectionFlow(
    selectedPhotoUris: List<Uri>,
    onPhotoAdded: (Uri) -> Boolean,
    onRemovePhoto: (Uri) -> Unit,
    onClearPhotos: () -> Unit,
    content: @Composable (
        selectedPhotoUris: List<Uri>,
        onAddPhotoClick: () -> Unit,
        onRemovePhoto: (Uri) -> Unit,
        onClearPhotos: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    var isSourceSheetVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val pendingCaptureUriState =
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    var pendingCaptureUriString by pendingCaptureUriState
    var shouldLaunchCamera by rememberSaveable {
        mutableStateOf(false)
    }
    var errorMessageResourceId by rememberSaveable {
        mutableStateOf<Int?>(null)
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
                return@rememberLauncherForActivityResult
            }

            if (!context.hasNonEmptyCapture(captureUri)) {
                deleteCaptureFile(context, captureUri)
                errorMessageResourceId = R.string.photo_camera_capture_failed
                isSourceSheetVisible = true
                return@rememberLauncherForActivityResult
            }

            if (selectedPhotoUris.size < MAX_RECORD_PHOTO_COUNT &&
                captureUri !in selectedPhotoUris &&
                onPhotoAdded(captureUri)
            ) {
                errorMessageResourceId = null
            } else {
                deleteCaptureFile(context, captureUri)
                errorMessageResourceId = R.string.photo_camera_limit_reached
                isSourceSheetVisible = true
            }
        }

    val pickPhotoLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { selectedUri ->
            if (selectedUri != null &&
                selectedPhotoUris.size < MAX_RECORD_PHOTO_COUNT &&
                selectedUri !in selectedPhotoUris
            ) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        selectedUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // 일부 Photo Picker 제공자는 앱 프로세스 수명 동안만 읽기 권한을 제공한다.
                }

                if (!onPhotoAdded(selectedUri)) {
                    releasePhoto(context, selectedUri)
                }
            }
        }

    LaunchedEffect(isSourceSheetVisible, shouldLaunchCamera) {
        if (!isSourceSheetVisible && shouldLaunchCamera) {
            shouldLaunchCamera = false
            val captureUri = pendingCaptureUriString?.let(Uri::parse)
            if (captureUri != null) {
                try {
                    captureLauncher.launch(captureUri)
                } catch (_: Exception) {
                    deleteCaptureFile(context, captureUri)
                    pendingCaptureUriString = null
                    errorMessageResourceId = R.string.photo_camera_unavailable
                    isSourceSheetVisible = true
                }
            }
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

    content(
        selectedPhotoUris,
        {
            if (selectedPhotoUris.size < MAX_RECORD_PHOTO_COUNT) {
                isSourceSheetVisible = true
            }
        },
        onRemovePhoto,
        onClearPhotos,
    )

    if (isSourceSheetVisible) {
        PhotoSourceBottomSheet(
            onDismissRequest = { isSourceSheetVisible = false },
            onCameraClick = {
                if (pendingCaptureUriString != null) {
                    return@PhotoSourceBottomSheet
                }

                isSourceSheetVisible = false
                val captureTarget = createPhotoCaptureTarget(context)
                if (captureTarget == null) {
                    errorMessageResourceId = R.string.photo_camera_file_unavailable
                    isSourceSheetVisible = true
                } else {
                    pendingCaptureUriString = captureTarget.uri.toString()
                    errorMessageResourceId = null
                    shouldLaunchCamera = true
                }
            },
            onGalleryClick = {
                isSourceSheetVisible = false
                pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            errorMessage = errorMessageResourceId?.let { stringResource(it) },
        )
    }
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
