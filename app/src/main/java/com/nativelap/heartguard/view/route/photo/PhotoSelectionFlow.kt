package com.nativelap.heartguard.view.route.photo

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
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
    onCameraClick: () -> Unit,
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

    val pickPhotoLauncher = rememberLauncherForActivityResult(
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
                isSourceSheetVisible = false
                onCameraClick()
            },
            onGalleryClick = {
                isSourceSheetVisible = false
                pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            errorMessage = null,
        )
    }
}
