package com.nativelap.heartguard.view.route.photo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.component.photo.PhotoSourceBottomSheet
import java.io.File
import java.util.UUID

/** Photo Route에 시스템 사진 획득·선택 상태와 2장 상한을 제공한다. */
@Composable
internal fun PhotoSelectionFlow(
    content: @Composable (
        selectedPhotoUris: List<Uri>,
        onAddPhotoClick: () -> Unit,
        onRemovePhoto: (Uri) -> Unit,
        onClearPhotos: () -> Unit,
    ) -> Unit,
) {
    val context = LocalContext.current
    val cameraUnavailableMessage = stringResource(R.string.photo_camera_unavailable)
    val selectedPhotoUrisStateSaver = listSaver<androidx.compose.runtime.MutableState<List<Uri>>, String>(
        save = { state -> state.value.map(Uri::toString) },
        restore = { restoredUris -> mutableStateOf(restoredUris.map(Uri::parse)) },
    )
    val selectedPhotoUrisState = rememberSaveable(saver = selectedPhotoUrisStateSaver) {
        mutableStateOf(emptyList())
    }
    var selectedPhotoUris by selectedPhotoUrisState
    var pendingCaptureUriString by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var isSourceSheetVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var sourceErrorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { wasSaved ->
        val pendingUri = pendingCaptureUriString?.let(Uri::parse)
        pendingCaptureUriString = null

        if (wasSaved && pendingUri != null) {
            selectedPhotoUris = (selectedPhotoUris + pendingUri).distinct()
        } else if (pendingUri != null) {
            deleteCaptureFile(context, pendingUri)
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { selectedUri ->
        if (selectedUri != null && selectedPhotoUris.size < MAX_SELECTED_PHOTOS) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                // 일부 Photo Picker 제공자는 앱 프로세스 수명 동안만 읽기 권한을 제공한다.
            }
            selectedPhotoUris = (selectedPhotoUris + selectedUri).distinct()
        }
    }

    val latestPhotoUris by rememberUpdatedState(selectedPhotoUris)
    val latestPendingCaptureUriString by rememberUpdatedState(pendingCaptureUriString)
    DisposableEffect(context) {
        onDispose {
            latestPhotoUris.forEach { uri ->
                releasePhoto(context, uri)
            }
            latestPendingCaptureUriString
                ?.let(Uri::parse)
                ?.let { uri -> deleteCaptureFile(context, uri) }
        }
    }

    content(
        selectedPhotoUris,
        {
            if (selectedPhotoUris.size < MAX_SELECTED_PHOTOS) {
                sourceErrorMessage = null
                isSourceSheetVisible = true
            }
        },
        { uri ->
            selectedPhotoUris = selectedPhotoUris - uri
            releasePhoto(context, uri)
        },
        {
            selectedPhotoUris.forEach { uri -> releasePhoto(context, uri) }
            selectedPhotoUris = emptyList()
        },
    )

    if (isSourceSheetVisible) {
        PhotoSourceBottomSheet(
            onDismissRequest = { isSourceSheetVisible = false },
            onCameraClick = {
                val captureUri = createCaptureUri(context)
                if (captureUri == null) {
                    sourceErrorMessage = cameraUnavailableMessage
                } else {
                    sourceErrorMessage = null
                    pendingCaptureUriString = captureUri.toString()
                    try {
                        takePictureLauncher.launch(captureUri)
                        isSourceSheetVisible = false
                    } catch (_: Exception) {
                        deleteCaptureFile(context, captureUri)
                        pendingCaptureUriString = null
                        sourceErrorMessage = cameraUnavailableMessage
                    }
                }
            },
            onGalleryClick = {
                sourceErrorMessage = null
                isSourceSheetVisible = false
                pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            errorMessage = sourceErrorMessage,
        )
    }
}

private fun createCaptureUri(context: Context): Uri? {
    val photoDirectory = File(context.cacheDir, PHOTO_CACHE_DIRECTORY)
    if (!photoDirectory.exists() && !photoDirectory.mkdirs()) {
        return null
    }

    val captureFile = File(photoDirectory, "$CAMERA_FILE_PREFIX${UUID.randomUUID()}.jpg")
    return try {
        if (!captureFile.createNewFile()) {
            return null
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            captureFile,
        )
    } catch (_: Exception) {
        captureFile.delete()
        null
    }
}

private fun releasePhoto(context: Context, uri: Uri) {
    if (isCameraCaptureUri(context, uri)) {
        deleteCaptureFile(context, uri)
        return
    }

    try {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    } catch (_: SecurityException) {
        // 임시 권한만 받은 선택 항목은 해제 가능한 영속 권한이 없다.
    }
}

private fun deleteCaptureFile(context: Context, uri: Uri) {
    if (!isCameraCaptureUri(context, uri)) {
        return
    }

    val fileName = uri.lastPathSegment ?: return
    if (!fileName.startsWith(CAMERA_FILE_PREFIX)) {
        return
    }

    File(File(context.cacheDir, PHOTO_CACHE_DIRECTORY), fileName).delete()
}

private fun isCameraCaptureUri(context: Context, uri: Uri): Boolean =
    uri.authority == "${context.packageName}.fileprovider"

private const val CAMERA_FILE_PREFIX = "heartguard_capture_"
private const val MAX_SELECTED_PHOTOS = 2
private const val PHOTO_CACHE_DIRECTORY = "photos"
