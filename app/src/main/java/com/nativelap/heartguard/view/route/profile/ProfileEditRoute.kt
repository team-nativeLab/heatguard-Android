package com.nativelap.heartguard.view.route.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.deleteCaptureFile
import com.nativelap.heartguard.view.component.profile.ProfilePhotoSheet
import com.nativelap.heartguard.view.route.photo.PhotoCameraCaptureResult
import com.nativelap.heartguard.view.route.photo.findActivity
import com.nativelap.heartguard.view.route.photo.rememberPhotoCameraCapture
import com.nativelap.heartguard.view.screen.profile.ProfileEditScreen
import com.nativelap.heartguard.viewmodel.profile.ProfileDefaultPhotoAction
import com.nativelap.heartguard.viewmodel.profile.ProfileEditEffect
import com.nativelap.heartguard.viewmodel.profile.ProfileEditScreenEvent
import com.nativelap.heartguard.viewmodel.profile.ProfileEditViewModel

/** 내 정보 수정 화면에 들어올 때마다 작업자 정보를 새로 조회하고, 이름 저장이 끝나면 "저장 완료"를 알린 뒤 이전 화면으로 돌아간다. */
@Composable
internal fun HeartGuardProfileEditRoute(
    onBackClick: () -> Unit,
    onPasswordChangeClick: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedMessage = stringResource(R.string.profile_saved)
    val photoDeleteTitle = stringResource(R.string.profile_photo_delete_title)
    val photoDeleteMessage = stringResource(R.string.profile_photo_delete_message)
    val photoDeleteConfirm = stringResource(R.string.profile_photo_delete_confirm)
    val photoDeleteCancel = stringResource(R.string.profile_photo_delete_cancel)
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    var isPhotoSheetVisible by rememberSaveable { mutableStateOf(false) }
    var isPhotoDeleteConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    // 이 화면이 카메라로 찍어 아직 서버에 올리지 않았거나 올리는 중인 사진의 URI다. 앨범에서 고른 사진은 넣지 않는다.
    val capturedPhotoUriState =
        rememberSaveable {
            mutableStateOf<String?>(null)
        }

    // 저장 중에는 composition 값이 늦을 수 있으므로 ViewModel의 최신 저장 상태를 읽는다.
    fun isSavingNow(): Boolean = viewModel.uiState.value.isSaving

    // 화면이 소유한 촬영 파일만 지운다. 앨범 사진과 서버 사진은 건드리지 않는다. 저장 중에는 업로드가 파일을 읽고 있으므로 지우지 않는다.
    fun discardCapturedPhoto() {
        if (isSavingNow()) {
            return
        }
        val capturedPhotoUri = capturedPhotoUriState.value?.let(Uri::parse)
        if (capturedPhotoUri != null) {
            deleteCaptureFile(context, capturedPhotoUri)
        }
        capturedPhotoUriState.value = null
    }

    fun discardPendingPhoto() {
        discardCapturedPhoto()
        viewModel.clearProfilePhoto()
    }

    val photoCapture =
        rememberPhotoCameraCapture(
            captureOwnerKey = "profile",
            isSourceSheetVisible = isPhotoSheetVisible,
            onCaptureResult = { captureResult ->
                when (captureResult) {
                    is PhotoCameraCaptureResult.Captured -> {
                        discardCapturedPhoto()
                        capturedPhotoUriState.value = captureResult.captureUri.toString()
                        viewModel.updateProfilePhoto(captureResult.captureUri)
                    }

                    PhotoCameraCaptureResult.Cancelled -> {
                        Unit
                    }

                    PhotoCameraCaptureResult.EmptyCapture -> {
                        viewModel.reportPhotoCaptureFailed()
                    }

                    PhotoCameraCaptureResult.LaunchFailed -> {
                        viewModel.reportPhotoCaptureFailed()
                    }
                }
            },
        )

    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selectedUri ->
            if (selectedUri != null) {
                discardCapturedPhoto()
                viewModel.updateProfilePhoto(selectedUri)
            }
        }

    // 사진 시트가 열려 있으면 뒤로가기는 시트만 닫고 선택한 사진은 그대로 둔다. 저장 중에는 항상 소비만 하고 아무 일도 하지 않는다.
    BackHandler(enabled = uiState.isSaving || (!isPhotoSheetVisible && !isPhotoDeleteConfirmationVisible)) {
        if (!isSavingNow()) {
            discardPendingPhoto()
            onBackClick()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.loadProfile()
    }

    // 저장 완료는 한 번만 처리해야 하는 결과라 화면이 보이는 동안에만 수집한다.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { profileEditEffect ->
                when (profileEditEffect) {
                    ProfileEditEffect.Saved -> {
                        // 서버에 올라간 촬영 사진은 더 이상 필요 없으므로 지운다.
                        discardCapturedPhoto()
                        viewModel.clearProfilePhoto()
                        Toast
                            .makeText(context, savedMessage, Toast.LENGTH_SHORT)
                            .show()
                        currentOnBackClick()
                    }
                }
            }
        }
    }

    DisposableEffect(context, viewModel) {
        onDispose {
            // 회전으로 화면이 다시 만들어질 때는 촬영 파일과 ViewModel 사진 상태를 남겨 rememberSaveable 상태와 맞춘다.
            if (context.findActivity()?.isChangingConfigurations != true) {
                discardCapturedPhoto()
                viewModel.clearProfilePhoto()
            }
        }
    }

    val onEvent: (ProfileEditScreenEvent) -> Unit = { event ->
        when (event) {
            ProfileEditScreenEvent.BackClicked -> {
                if (!isSavingNow()) {
                    discardPendingPhoto()
                    onBackClick()
                }
            }

            is ProfileEditScreenEvent.NameChanged -> {
                viewModel.updateName(event.name)
            }

            ProfileEditScreenEvent.SaveClicked -> {
                if (!photoCapture.isSavingCapture) {
                    viewModel.saveProfile()
                }
            }

            ProfileEditScreenEvent.RetryClicked -> {
                viewModel.loadProfile()
            }

            ProfileEditScreenEvent.PasswordChangeClicked -> {
                if (!isSavingNow()) {
                    discardPendingPhoto()
                    onPasswordChangeClick()
                }
            }

            ProfileEditScreenEvent.ProfilePhotoClicked -> {
                if (!isSavingNow() && !photoCapture.isSavingCapture) {
                    isPhotoSheetVisible = true
                }
            }

            ProfileEditScreenEvent.ProfilePhotoSheetDismissed -> {
                isPhotoSheetVisible = false
            }

            ProfileEditScreenEvent.ProfilePhotoCaptureSelected -> {
                isPhotoSheetVisible = false
                if (!photoCapture.requestCapture()) {
                    viewModel.reportPhotoCaptureFailed()
                }
            }

            ProfileEditScreenEvent.ProfilePhotoChangeSelected -> {
                if (!photoCapture.isSavingCapture) {
                    isPhotoSheetVisible = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }

            ProfileEditScreenEvent.ProfilePhotoDeleteSelected -> {
                isPhotoSheetVisible = false
                if (!isSavingNow() && !photoCapture.isSavingCapture) {
                    when (uiState.defaultPhotoAction) {
                        ProfileDefaultPhotoAction.ConfirmDeletion -> {
                            isPhotoDeleteConfirmationVisible = true
                        }

                        ProfileDefaultPhotoAction.ClearSelection -> {
                            discardPendingPhoto()
                        }

                        ProfileDefaultPhotoAction.None -> {
                            Unit
                        }
                    }
                }
            }
        }
    }

    ProfileEditScreen(
        uiState = uiState,
        profilePhotoUri = uiState.profilePhotoUri,
        onEvent = onEvent,
    )

    if (isPhotoSheetVisible) {
        ProfilePhotoSheet(
            onDismissRequest = { onEvent(ProfileEditScreenEvent.ProfilePhotoSheetDismissed) },
            onCaptureClick = { onEvent(ProfileEditScreenEvent.ProfilePhotoCaptureSelected) },
            onAlbumClick = { onEvent(ProfileEditScreenEvent.ProfilePhotoChangeSelected) },
            onDefaultPhotoClick = { onEvent(ProfileEditScreenEvent.ProfilePhotoDeleteSelected) },
        )
    }

    if (isPhotoDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isPhotoDeleteConfirmationVisible = false },
            title = { Text(photoDeleteTitle) },
            text = { Text(photoDeleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isPhotoDeleteConfirmationVisible = false
                        discardCapturedPhoto()
                        viewModel.requestProfilePhotoDeletion()
                        viewModel.saveProfile()
                    },
                ) {
                    Text(photoDeleteConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { isPhotoDeleteConfirmationVisible = false }) {
                    Text(photoDeleteCancel)
                }
            },
        )
    }
}
