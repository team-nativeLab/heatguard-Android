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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.nativelap.heartguard.core.util.createPhotoCaptureTarget
import com.nativelap.heartguard.core.util.deleteCaptureFile

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
) {
    val hasPendingCapture: Boolean
        get() = pendingCaptureUriState.value != null

    /** 촬영용 파일을 만들고, 시트가 닫히면 카메라를 연다. 파일을 만들지 못하면 false를 반환한다. */
    fun requestCapture(): Boolean {
        val captureTarget = createPhotoCaptureTarget(context) ?: return false

        val staleCaptureUri = pendingCaptureUriState.value?.let(Uri::parse)
        if (staleCaptureUri != null) {
            deleteCaptureFile(context, staleCaptureUri)
        }

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
    val currentOnCaptureResult by rememberUpdatedState(onCaptureResult)

    val captureLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { wasCaptureSuccessful ->
            val captureUri = pendingCaptureUriState.value?.let(Uri::parse)
            pendingCaptureUriState.value = null

            if (captureUri == null) {
                return@rememberLauncherForActivityResult
            }

            if (!wasCaptureSuccessful) {
                deleteCaptureFile(context, captureUri)
                currentOnCaptureResult(PhotoCameraCaptureResult.Cancelled)
                return@rememberLauncherForActivityResult
            }

            if (!context.hasNonEmptyCapture(captureUri)) {
                deleteCaptureFile(context, captureUri)
                currentOnCaptureResult(PhotoCameraCaptureResult.EmptyCapture)
                return@rememberLauncherForActivityResult
            }

            currentOnCaptureResult(PhotoCameraCaptureResult.Captured(captureUri))
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
                pendingCaptureUriState.value
                    ?.let(Uri::parse)
                    ?.let { captureUri -> deleteCaptureFile(context, captureUri) }
            }
        }
    }

    return remember(
        context,
        pendingCaptureUriState,
        shouldLaunchCameraState,
    ) {
        PhotoCameraCapture(
            context = context,
            pendingCaptureUriState = pendingCaptureUriState,
            shouldLaunchCameraState = shouldLaunchCameraState,
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
