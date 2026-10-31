package com.nativelap.heartguard.view.route.photo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoSelectionFlowCameraTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cameraButtonLaunchesSystemCameraAndCancelKeepsPhotoScreenAfterRestore() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris = mutableStateListOf<Uri>()
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            val baseContext = LocalContext.current
            val configurationContext =
                remember {
                    ConfigurationChangingActivity(baseContext)
                }
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
                LocalContext provides configurationContext,
            ) {
                PhotoSelectionFlowTestContent(selectedPhotoUris)
            }
        }

        composeRule.onNodeWithText("사진 추가").performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.photo_source_camera))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            registryOwner.activityResultRegistry.launches.isNotEmpty()
        }

        val cameraLaunch = registryOwner.activityResultRegistry.launches.single()
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, cameraLaunch.intent.action)
        assertEquals(
            cameraLaunch.captureUri,
            cameraLaunch.intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java),
        )
        assertTrue(isCaptureReadable(cameraLaunch.captureUri))

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("현재 사진 입력 화면").assertExists()
        assertEquals(1, registryOwner.activityResultRegistry.launches.size)
        assertTrue(isCaptureReadable(cameraLaunch.captureUri))

        registryOwner.activityResultRegistry.dispatchCameraResult(wasCaptureSuccessful = false)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !isCaptureReadable(cameraLaunch.captureUri)
        }
        composeRule.onNodeWithText("현재 사진 입력 화면").assertExists()
        assertTrue(selectedPhotoUris.isEmpty())
    }

    @Test
    fun successfulCaptureAddsPhotoToCurrentScreen() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris = mutableStateListOf<Uri>()

        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                PhotoSelectionFlowTestContent(selectedPhotoUris)
            }
        }

        composeRule.onNodeWithText("사진 추가").performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.photo_source_camera))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            registryOwner.activityResultRegistry.launches.isNotEmpty()
        }

        val cameraLaunch = registryOwner.activityResultRegistry.launches.single()
        context.contentResolver.openOutputStream(cameraLaunch.captureUri)?.use { outputStream ->
            outputStream.write(byteArrayOf(1, 2, 3))
        }
        registryOwner.activityResultRegistry.dispatchCameraResult(wasCaptureSuccessful = true)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            selectedPhotoUris.size == 1
        }
        composeRule.onNodeWithText("현재 사진 입력 화면").assertExists()
        composeRule.onNodeWithText("사진 수: 1").assertExists()
        assertTrue(isCaptureReadable(cameraLaunch.captureUri))
    }

    @Test
    fun successfulCameraResultWithEmptyFileDoesNotAddPhotoAndDeletesCapture() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris = mutableStateListOf<Uri>()

        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                PhotoSelectionFlowTestContent(selectedPhotoUris)
            }
        }

        openCameraFromPhotoScreen(registryOwner.activityResultRegistry)
        val cameraLaunch = registryOwner.activityResultRegistry.launches.single()
        assertTrue(isCaptureReadable(cameraLaunch.captureUri))

        registryOwner.activityResultRegistry.dispatchCameraResult(wasCaptureSuccessful = true)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !isCaptureReadable(cameraLaunch.captureUri)
        }
        composeRule
            .onNodeWithText(context.getString(R.string.photo_camera_capture_failed))
            .assertExists()
        composeRule.onNodeWithText("현재 사진 입력 화면").assertExists()
        composeRule.onNodeWithText("사진 수: 0").assertExists()
        assertTrue(selectedPhotoUris.isEmpty())
    }

    @Test
    fun reachingMaximumPhotoCountBlocksCameraSheetAndLaunch() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris =
            mutableStateListOf(
                Uri.parse("content://existing-photo/one"),
                Uri.parse("content://existing-photo/two"),
            )

        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                PhotoSelectionFlowTestContent(selectedPhotoUris)
            }
        }

        composeRule.onNodeWithText("사진 추가").performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.photo_source_camera))
            .assertDoesNotExist()
        assertTrue(registryOwner.activityResultRegistry.launches.isEmpty())
    }

    @Test
    fun launcherFailureDeletesCaptureAndKeepsPhotoScreen() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris = mutableStateListOf<Uri>()
        registryOwner.activityResultRegistry.failLaunch = true

        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                PhotoSelectionFlowTestContent(selectedPhotoUris)
            }
        }

        openCameraFromPhotoScreen(registryOwner.activityResultRegistry)

        val cameraLaunch = registryOwner.activityResultRegistry.launches.single()
        composeRule
            .onNodeWithText(context.getString(R.string.photo_camera_unavailable))
            .assertExists()
        composeRule.onNodeWithText("현재 사진 입력 화면").assertExists()
        assertFalse(isCaptureReadable(cameraLaunch.captureUri))
        assertTrue(selectedPhotoUris.isEmpty())
    }

    @Test
    fun disposingPhotoSelectionFlowDeletesPendingCapture() {
        val registryOwner = TestActivityResultRegistryOwner(context)
        val selectedPhotoUris = mutableStateListOf<Uri>()

        composeRule.setContent {
            val isPhotoFlowVisible = remember { mutableStateOf(true) }
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                Column {
                    if (isPhotoFlowVisible.value) {
                        PhotoSelectionFlowTestContent(selectedPhotoUris)
                    } else {
                        Text(text = "사진 화면을 닫았습니다")
                    }

                    Button(
                        onClick = { isPhotoFlowVisible.value = false },
                    ) {
                        Text(text = "사진 화면 종료")
                    }
                }
            }
        }

        openCameraFromPhotoScreen(registryOwner.activityResultRegistry)
        val cameraLaunch = registryOwner.activityResultRegistry.launches.single()
        assertTrue(isCaptureReadable(cameraLaunch.captureUri))

        composeRule.onNodeWithText("사진 화면 종료").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !isCaptureReadable(cameraLaunch.captureUri)
        }
        composeRule.onNodeWithText("사진 화면을 닫았습니다").assertExists()
        composeRule.onNodeWithText("현재 사진 입력 화면").assertDoesNotExist()
    }

    private fun openCameraFromPhotoScreen(registry: RecordingActivityResultRegistry) {
        composeRule.onNodeWithText("사진 추가").performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.photo_source_camera))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            registry.launches.isNotEmpty()
        }
    }

    private fun isCaptureReadable(uri: Uri): Boolean =
        try {
            context.contentResolver.openInputStream(uri)?.use { true } == true
        } catch (_: Exception) {
            false
        }
}

@androidx.compose.runtime.Composable
private fun PhotoSelectionFlowTestContent(selectedPhotoUris: MutableList<Uri>) {
    PhotoSelectionFlow(
        selectedPhotoUris = selectedPhotoUris,
        onPhotoAdded = { photoUri -> selectedPhotoUris.add(photoUri) },
        onRemovePhoto = selectedPhotoUris::remove,
        onClearPhotos = selectedPhotoUris::clear,
    ) { selectedUris, onAddPhotoClick, _, _ ->
        Column {
            Text(text = "현재 사진 입력 화면")
            Text(text = "사진 수: ${selectedUris.size}")
            Button(onClick = onAddPhotoClick) {
                Text(text = "사진 추가")
            }
        }
    }
}

private class TestActivityResultRegistryOwner(
    context: Context,
) : ActivityResultRegistryOwner {
    override val activityResultRegistry = RecordingActivityResultRegistry(context)
}

private class RecordingActivityResultRegistry(
    private val context: Context,
) : ActivityResultRegistry() {
    val launches = mutableListOf<CameraLaunch>()
    var failLaunch = false

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        val captureUri = input as? Uri ?: error("Expected a capture URI")
        launches +=
            CameraLaunch(
                requestCode = requestCode,
                captureUri = captureUri,
                intent = contract.createIntent(context, input),
            )
        if (failLaunch) {
            throw IllegalStateException("Fake camera launch failure")
        }
    }

    fun dispatchCameraResult(wasCaptureSuccessful: Boolean) {
        val requestCode = launches.lastOrNull()?.requestCode ?: error("No camera launch recorded")
        dispatchResult(requestCode, wasCaptureSuccessful)
    }
}

private class ConfigurationChangingActivity(
    baseContext: Context,
) : Activity() {
    init {
        attachBaseContext(baseContext)
    }

    override fun isChangingConfigurations(): Boolean = true
}

private data class CameraLaunch(
    val requestCode: Int,
    val captureUri: Uri,
    val intent: Intent,
)
