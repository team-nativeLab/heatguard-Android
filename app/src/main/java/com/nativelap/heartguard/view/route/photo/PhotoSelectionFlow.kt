package com.nativelap.heartguard.view.route.photo

import android.content.Context
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.PHOTO_CACHE_DIRECTORY
import com.nativelap.heartguard.core.util.PHOTO_CAPTURE_FILE_PREFIX
import com.nativelap.heartguard.core.util.deleteCaptureFile
import com.nativelap.heartguard.core.util.releasePhoto
import com.nativelap.heartguard.view.component.photo.PhotoSourceBottomSheet
import java.io.File
import java.util.UUID

/** Photo Route에 시스템 사진 획득·선택 상태와 2장 상한을 제공한다.
 * [initialPhotoUris]로 상위(대개 [com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel])가
 * 갖고 있던 값을 이어받고, 목록이 바뀔 때마다 [onPhotosChanged]로 그 값을 다시 올려보내
 * 화면을 벗어났다 돌아와도(예: 현장 사진 → 저장 전 확인) 선택한 사진이 유지되게 한다. */
@Composable
internal fun PhotoSelectionFlow(
    initialPhotoUris: List<Uri>,
    onPhotosChanged: (List<Uri>) -> Unit,
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
        mutableStateOf(initialPhotoUris)
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

    LaunchedEffect(selectedPhotoUris) {
        onPhotosChanged(selectedPhotoUris)
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

    // 확정된 selectedPhotoUris는 더 이상 여기서 정리하지 않는다 — 소유권이 RecordDraftViewModel로
    // 넘어가 기록 흐름 전체가 끝날 때(reset()/onCleared()) 한 번만 정리된다. 이 Route가 떠 있는 동안
    // 아직 결과를 받지 못한 채 남아있는 "진행 중인 촬영" 파일만 여기서 정리한다.
    val latestPendingCaptureUriString by rememberUpdatedState(pendingCaptureUriString)
    DisposableEffect(context) {
        onDispose {
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

    val captureFile = File(photoDirectory, "$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID()}.jpg")
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

private const val MAX_SELECTED_PHOTOS = 2
