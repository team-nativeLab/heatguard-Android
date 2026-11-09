package com.nativelap.heartguard.view.route.photo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.createPhotoCaptureTarget
import com.nativelap.heartguard.core.util.deleteCaptureFile
import kotlinx.coroutines.delay

/** 시스템 카메라 촬영 한 번의 결과다. 취소·실패·빈 파일은 임시 파일을 이미 지운 뒤 알린다. */
internal sealed interface PhotoCameraCaptureResult {
    /** 내용이 있는 촬영 사진이다. 이후 파일을 쓰거나 지우는 책임은 호출자에게 넘어간다. */
    data class Captured(
        val captureUri: Uri,
    ) : PhotoCameraCaptureResult

    data object Cancelled : PhotoCameraCaptureResult

    data object EmptyCapture : PhotoCameraCaptureResult

    data object LaunchFailed : PhotoCameraCaptureResult
}

/** 촬영 요청을 화면 상태로 보관하고 카메라를 여는 진입점이다. 회전 후에도 대기 중인 촬영을 이어간다. */
internal class PhotoCameraCapture(
    private val context: Context,
    private val pendingCaptureUriState: MutableState<String?>,
    private val shouldLaunchCameraState: MutableState<Boolean>,
    private val isSavingCaptureState: MutableState<Boolean>,
    private val isRecoveringCaptureState: MutableState<Boolean>,
    private val captureGenerationState: MutableState<Int>,
) {
    val hasPendingCapture: Boolean
        get() = shouldLaunchCameraState.value || isSavingCaptureState.value || isRecoveringCaptureState.value

    val isSavingCapture: Boolean
        get() = isSavingCaptureState.value || isRecoveringCaptureState.value

    /** 촬영용 파일을 만들고, 시트가 닫히면 카메라를 연다. 파일을 만들지 못하면 false를 반환한다. */
    fun requestCapture(): Boolean {
        if (hasPendingCapture) {
            return false
        }
        val captureTarget = createPhotoCaptureTarget(context) ?: return false

        val staleCaptureUri = pendingCaptureUriState.value?.let(Uri::parse)
        if (staleCaptureUri != null) {
            deleteCaptureFile(context, staleCaptureUri)
        }

        captureGenerationState.value += 1
        pendingCaptureUriState.value = captureTarget.uri.toString()
        shouldLaunchCameraState.value = true
        return true
    }
}

/**
 * 시스템 카메라 촬영 흐름을 만든다. [isSourceSheetVisible]이 false가 된 뒤 카메라를 열고,
 * 결과는 [onCaptureResult]로 한 번씩 전달한다. 화면을 떠나면 대기 중인 임시 파일을 지운다(회전 제외).
 */
@Composable
internal fun rememberPhotoCameraCapture(
    captureOwnerKey: String,
    isSourceSheetVisible: Boolean,
    onCaptureResult: (PhotoCameraCaptureResult) -> Unit,
): PhotoCameraCapture {
    val context = LocalContext.current
    val pendingCaptureUriState =
        rememberSaveable {
            mutableStateOf<String?>(null)
        }
    val shouldLaunchCameraState =
        rememberSaveable {
            mutableStateOf(false)
        }
    val isSavingCaptureState = rememberSaveable { mutableStateOf(false) }
    val isRecoveryPromptVisibleState = rememberSaveable { mutableStateOf(false) }
    val captureGenerationState = rememberSaveable { mutableStateOf(0) }
    val isRecoveringCaptureState =
        remember {
            mutableStateOf(
                pendingCaptureUriState.value != null &&
                    !shouldLaunchCameraState.value &&
                    !isSavingCaptureState.value,
            )
        }
    val lifecycleOwner = LocalLifecycleOwner.current
    val gallerySaveViewModel: PhotoGallerySaveViewModel =
        viewModel(
            key = "photo-gallery-$captureOwnerKey",
            factory = PhotoGallerySaveViewModel.factory(context),
        )
    val gallerySaveState by gallerySaveViewModel.saveState.collectAsState()
    val currentOnCaptureResult by rememberUpdatedState(onCaptureResult)

    val captureGeneration = captureGenerationState.value
    val captureLauncher =
        key(captureGeneration) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture(),
            ) { wasCaptureSuccessful ->
                if (captureGeneration != captureGenerationState.value) {
                    return@rememberLauncherForActivityResult
                }
                isRecoveringCaptureState.value = false
                isRecoveryPromptVisibleState.value = false
                val captureUri = pendingCaptureUriState.value?.let(Uri::parse)
                if (captureUri == null) {
                    return@rememberLauncherForActivityResult
                }

                if (!wasCaptureSuccessful) {
                    pendingCaptureUriState.value = null
                    deleteCaptureFile(context, captureUri)
                    currentOnCaptureResult(PhotoCameraCaptureResult.Cancelled)
                    return@rememberLauncherForActivityResult
                }

                if (!context.hasNonEmptyCapture(captureUri)) {
                    pendingCaptureUriState.value = null
                    deleteCaptureFile(context, captureUri)
                    currentOnCaptureResult(PhotoCameraCaptureResult.EmptyCapture)
                    return@rememberLauncherForActivityResult
                }

                isSavingCaptureState.value = true
                gallerySaveViewModel.save(captureUri)
            }
        }

    LaunchedEffect(lifecycleOwner, isRecoveringCaptureState.value) {
        if (isRecoveringCaptureState.value && !isRecoveryPromptVisibleState.value) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Activity Result 복원 콜백이 먼저 도착할 기회를 준다.
                delay(1_000)
                if (isRecoveringCaptureState.value && pendingCaptureUriState.value != null) {
                    isRecoveryPromptVisibleState.value = true
                }
            }
        }
    }

    LaunchedEffect(gallerySaveState, isSavingCaptureState.value) {
        if (!isSavingCaptureState.value) {
            return@LaunchedEffect
        }
        when (val saveState = gallerySaveState) {
            GallerySaveState.Idle -> {
                pendingCaptureUriState.value?.let(Uri::parse)?.let(gallerySaveViewModel::save)
            }

            GallerySaveState.Saving -> {
                Unit
            }

            is GallerySaveState.Completed -> {
                pendingCaptureUriState.value = null
                isSavingCaptureState.value = false
                gallerySaveViewModel.consume()
                if (!saveState.saved) {
                    Toast.makeText(context, R.string.photo_gallery_save_failed, Toast.LENGTH_LONG).show()
                }
                currentOnCaptureResult(PhotoCameraCaptureResult.Captured(saveState.captureUri))
            }
        }
    }

    val shouldLaunchCamera = shouldLaunchCameraState.value
    LaunchedEffect(isSourceSheetVisible, shouldLaunchCamera) {
        if (!isSourceSheetVisible && shouldLaunchCamera) {
            shouldLaunchCameraState.value = false
            val captureUri = pendingCaptureUriState.value?.let(Uri::parse)
            if (captureUri != null) {
                try {
                    captureLauncher.launch(captureUri)
                } catch (_: Exception) {
                    deleteCaptureFile(context, captureUri)
                    pendingCaptureUriState.value = null
                    currentOnCaptureResult(PhotoCameraCaptureResult.LaunchFailed)
                }
            }
        }
    }

    DisposableEffect(context) {
        onDispose {
            if (context.findActivity()?.isChangingConfigurations != true) {
                if (isSavingCaptureState.value) {
                    gallerySaveViewModel.abandonSave()
                } else {
                    pendingCaptureUriState.value
                        ?.let(Uri::parse)
                        ?.let { captureUri -> deleteCaptureFile(context, captureUri) }
                }
            }
        }
    }

    if (isSavingCaptureState.value || (isRecoveringCaptureState.value && !isRecoveryPromptVisibleState.value)) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    stringResource(
                        if (isRecoveringCaptureState.value) {
                            R.string.photo_capture_recovery_checking
                        } else {
                            R.string.photo_gallery_saving
                        },
                    ),
                )
            },
            text = {
                Row {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.photo_gallery_saving_description))
                }
            },
        )
    }

    if (isRecoveryPromptVisibleState.value) {
        val captureUri = pendingCaptureUriState.value?.let(Uri::parse)
        val canRecoverPhoto = captureUri != null && context.hasNonEmptyCapture(captureUri)
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.photo_capture_recovery_title)) },
            text = {
                Text(
                    stringResource(
                        if (canRecoverPhoto) {
                            R.string.photo_capture_recovery_available
                        } else {
                            R.string.photo_capture_recovery_empty
                        },
                    ),
                )
            },
            confirmButton = {
                if (canRecoverPhoto) {
                    TextButton(
                        onClick = {
                            isRecoveryPromptVisibleState.value = false
                            isRecoveringCaptureState.value = false
                            captureGenerationState.value += 1
                            isSavingCaptureState.value = true
                            gallerySaveViewModel.save(captureUri)
                        },
                    ) {
                        Text(stringResource(R.string.photo_capture_recovery_use))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        captureUri?.let { uri -> deleteCaptureFile(context, uri) }
                        pendingCaptureUriState.value = null
                        captureGenerationState.value += 1
                        isRecoveryPromptVisibleState.value = false
                        isRecoveringCaptureState.value = false
                    },
                ) {
                    Text(
                        stringResource(
                            if (canRecoverPhoto) {
                                R.string.photo_capture_recovery_discard
                            } else {
                                R.string.photo_capture_recovery_confirm
                            },
                        ),
                    )
                }
            },
        )
    }

    return remember(
        context,
        pendingCaptureUriState,
        shouldLaunchCameraState,
        isSavingCaptureState,
        isRecoveringCaptureState,
        captureGenerationState,
    ) {
        PhotoCameraCapture(
            context = context,
            pendingCaptureUriState = pendingCaptureUriState,
            shouldLaunchCameraState = shouldLaunchCameraState,
            isSavingCaptureState = isSavingCaptureState,
            isRecoveringCaptureState = isRecoveringCaptureState,
            captureGenerationState = captureGenerationState,
        )
    }
}

/** 회전처럼 Activity가 다시 만들어지는지 확인하려고 감싼 Context에서 Activity를 찾는다. */
internal fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext !is Activity && currentContext is ContextWrapper) {
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}

private fun Context.hasNonEmptyCapture(uri: Uri): Boolean =
    try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.read() != -1
        } ?: false
    } catch (_: Exception) {
        false
    }
