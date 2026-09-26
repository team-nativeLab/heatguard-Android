package com.nativelap.heartguard.view.route.photo

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nativelap.heartguard.R
import com.nativelap.heartguard.core.util.createPhotoCaptureTarget
import com.nativelap.heartguard.core.util.deleteCaptureFile
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.screen.photo.PhotoCameraScreen
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import java.util.concurrent.atomic.AtomicBoolean

/** 권한, 카메라 생명주기, 임시 파일 소유권을 관리하고 촬영 결과를 기록 초안에 전달한다. */
@Composable
internal fun HeartGuardPhotoCameraRoute(
    recordDraftViewModel: RecordDraftViewModel,
    recordType: RecordType,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraWindow = remember(context) {
        context.findActivity()?.window
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasRequestedCameraPermission by rememberSaveable {
        mutableStateOf(false)
    }
    var isCameraReady by remember {
        mutableStateOf(false)
    }
    var isCapturing by remember {
        mutableStateOf(false)
    }
    var cameraErrorResourceId by remember {
        mutableStateOf<Int?>(null)
    }
    var bindingAttempt by remember {
        mutableIntStateOf(0)
    }
    var pendingCaptureUriString by remember {
        mutableStateOf<String?>(null)
    }
    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }
    val isRouteDisposed = remember {
        AtomicBoolean(false)
    }
    val latestOnBackClick by rememberUpdatedState(onBackClick)
    val latestPendingCaptureUriString by rememberUpdatedState(pendingCaptureUriString)
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(cameraWindow) {
        val window = cameraWindow
        if (window == null) {
            onDispose { }
        } else {
            val systemBarsController = WindowInsetsControllerCompat(window, window.decorView)
            val wasLightStatusBar = systemBarsController.isAppearanceLightStatusBars
            val wasLightNavigationBar = systemBarsController.isAppearanceLightNavigationBars
            systemBarsController.isAppearanceLightStatusBars = false
            systemBarsController.isAppearanceLightNavigationBars = false

            onDispose {
                systemBarsController.isAppearanceLightStatusBars = wasLightStatusBar
                systemBarsController.isAppearanceLightNavigationBars = wasLightNavigationBar
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isPermissionGranted ->
        hasCameraPermission = isPermissionGranted
        cameraErrorResourceId = if (isPermissionGranted) {
            null
        } else {
            R.string.photo_camera_permission_denied
        }
    }

    LaunchedEffect(hasCameraPermission, hasRequestedCameraPermission) {
        if (!hasCameraPermission && !hasRequestedCameraPermission) {
            hasRequestedCameraPermission = true
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val permissionObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val isPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                hasCameraPermission = isPermissionGranted
                if (isPermissionGranted) {
                    cameraErrorResourceId = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(permissionObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(permissionObserver)
        }
    }

    DisposableEffect(
        hasCameraPermission,
        lifecycleOwner,
        previewView,
        bindingAttempt,
    ) {
        if (!hasCameraPermission) {
            isCameraReady = false
            imageCapture = null
            onDispose { }
        } else {
            var isDisposed = false
            var boundCameraProvider: ProcessCameraProvider? = null
            var boundPreview: Preview? = null
            var boundImageCapture: ImageCapture? = null
            isCameraReady = false
            cameraErrorResourceId = null

            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener(
                    {
                        if (isDisposed) {
                            return@addListener
                        }

                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val previewUseCase = Preview.Builder().build().also { preview ->
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageCaptureUseCase = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            boundCameraProvider = cameraProvider
                            boundPreview = previewUseCase
                            boundImageCapture = imageCaptureUseCase
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                imageCaptureUseCase,
                            )

                            imageCapture = imageCaptureUseCase
                            isCameraReady = true
                        } catch (_: Exception) {
                            cameraErrorResourceId = R.string.photo_camera_unavailable
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            } catch (_: Exception) {
                cameraErrorResourceId = R.string.photo_camera_unavailable
            }

            onDispose {
                isDisposed = true
                isCameraReady = false
                imageCapture = null
                val cameraProvider = boundCameraProvider
                val previewUseCase = boundPreview
                val imageCaptureUseCase = boundImageCapture
                if (cameraProvider != null &&
                    previewUseCase != null &&
                    imageCaptureUseCase != null
                ) {
                    cameraProvider.unbind(previewUseCase, imageCaptureUseCase)
                }
            }
        }
    }

    // 촬영 도중 사용자가 화면을 닫으면 아직 ViewModel에 넘기지 않은 임시 사진을 제거한다.
    DisposableEffect(context) {
        onDispose {
            latestPendingCaptureUriString
                ?.let(Uri::parse)
                ?.let { captureUri -> deleteCaptureFile(context, captureUri) }
        }
    }

    val screenTitle = when (recordType) {
        RecordType.TEMPERATURE -> stringResource(R.string.photo_camera_field_title)
        RecordType.WORK -> stringResource(R.string.photo_camera_work_title)
        RecordType.REST -> stringResource(R.string.photo_camera_rest_title)
    }
    val isPermissionDenied = !hasCameraPermission && hasRequestedCameraPermission
    val errorMessageResourceId = if (isPermissionDenied) {
        R.string.photo_camera_permission_denied
    } else {
        cameraErrorResourceId
    }
    val errorMessage = errorMessageResourceId?.let { messageResourceId ->
        stringResource(messageResourceId)
    }

    PhotoCameraScreen(
        previewView = previewView,
        title = screenTitle,
        hasCameraPermission = hasCameraPermission,
        isCameraReady = isCameraReady,
        isCapturing = isCapturing,
        isPermissionDenied = isPermissionDenied,
        errorMessage = errorMessage,
        onBackClick = onBackClick,
        onRequestPermissionClick = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onOpenSettingsClick = {
            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(settingsIntent)
            } catch (_: Exception) {
                cameraErrorResourceId = R.string.photo_camera_settings_unavailable
            }
        },
        onRetryClick = {
            cameraErrorResourceId = null
            bindingAttempt += 1
        },
        onCaptureClick = {
            val activeImageCapture = imageCapture
            if (activeImageCapture != null && !isCapturing) {
                val captureTarget = createPhotoCaptureTarget(context)
                if (captureTarget == null) {
                    cameraErrorResourceId = R.string.photo_camera_file_unavailable
                } else {
                    activeImageCapture.targetRotation = previewView.display?.rotation
                        ?: Surface.ROTATION_0
                    pendingCaptureUriString = captureTarget.uri.toString()
                    isCapturing = true
                    try {
                        activeImageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(captureTarget.file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(
                                    outputFileResults: ImageCapture.OutputFileResults,
                                ) {
                                    if (isRouteDisposed.get()) {
                                        deleteCaptureFile(context, captureTarget.uri)
                                        return
                                    }

                                    val captureUri = pendingCaptureUriString?.let(Uri::parse)
                                    if (captureUri == null) {
                                        captureTarget.file.delete()
                                        isCapturing = false
                                        return
                                    }

                                    val isPhotoAccepted = recordDraftViewModel.addPhoto(
                                        recordType,
                                        captureUri,
                                    )
                                    pendingCaptureUriString = null
                                    isCapturing = false

                                    if (isPhotoAccepted) {
                                        latestOnBackClick()
                                    } else {
                                        deleteCaptureFile(context, captureUri)
                                        cameraErrorResourceId = R.string.photo_camera_limit_reached
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    if (isRouteDisposed.get()) {
                                        deleteCaptureFile(context, captureTarget.uri)
                                        return
                                    }

                                    pendingCaptureUriString
                                        ?.let(Uri::parse)
                                        ?.let { captureUri -> deleteCaptureFile(context, captureUri) }
                                    pendingCaptureUriString = null
                                    isCapturing = false
                                    cameraErrorResourceId = R.string.photo_camera_capture_failed
                                }
                            },
                        )
                    } catch (_: Exception) {
                        deleteCaptureFile(context, captureTarget.uri)
                        pendingCaptureUriString = null
                        isCapturing = false
                        cameraErrorResourceId = R.string.photo_camera_capture_failed
                    }
                }
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            isRouteDisposed.set(true)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext !is Activity && currentContext is ContextWrapper) {
        currentContext = currentContext.baseContext
    }
    return currentContext as? Activity
}
