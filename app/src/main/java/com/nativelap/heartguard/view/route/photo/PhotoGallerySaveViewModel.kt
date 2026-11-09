package com.nativelap.heartguard.view.route.photo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.util.deleteCaptureFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal sealed interface GallerySaveState {
    data object Idle : GallerySaveState

    data object Saving : GallerySaveState

    data class Completed(
        val captureUri: Uri,
        val saved: Boolean,
    ) : GallerySaveState
}

internal class PhotoGallerySaveViewModel(
    context: Context,
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val mutableSaveState = MutableStateFlow<GallerySaveState>(GallerySaveState.Idle)
    val saveState = mutableSaveState.asStateFlow()
    private var isAbandoned = false

    fun save(captureUri: Uri) {
        if (mutableSaveState.value != GallerySaveState.Idle) {
            return
        }
        isAbandoned = false
        mutableSaveState.value = GallerySaveState.Saving
        viewModelScope.launch {
            try {
                val saved = saveCaptureToGallery(applicationContext, captureUri)
                if (!isAbandoned) {
                    mutableSaveState.value = GallerySaveState.Completed(captureUri, saved)
                }
            } finally {
                if (isAbandoned) {
                    deleteCaptureFile(applicationContext, captureUri)
                    mutableSaveState.value = GallerySaveState.Idle
                }
            }
        }
    }

    fun abandonSave() {
        isAbandoned = true
        val completedSave = mutableSaveState.value as? GallerySaveState.Completed
        if (completedSave != null) {
            deleteCaptureFile(applicationContext, completedSave.captureUri)
            mutableSaveState.value = GallerySaveState.Idle
        }
    }

    fun consume() {
        mutableSaveState.value = GallerySaveState.Idle
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PhotoGallerySaveViewModel(context.applicationContext) as T
            }
    }
}

private suspend fun saveCaptureToGallery(
    context: Context,
    captureUri: Uri,
): Boolean =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var galleryUri: Uri? = null
        var published = false
        try {
            val displayName = captureUri.lastPathSegment ?: return@withContext false
            val relativePath = "${Environment.DIRECTORY_PICTURES}/HeartGuard/"
            val existingPhoto =
                resolver
                    .query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Images.Media._ID),
                        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.IS_PENDING} = 0",
                        arrayOf(displayName, relativePath),
                        null,
                    )?.use { cursor -> cursor.moveToFirst() } == true
            if (existingPhoto) {
                return@withContext true
            }
            val attributes =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            galleryUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, attributes)
                ?: return@withContext false
            val destinationUri = galleryUri
            val inputStream = resolver.openInputStream(captureUri) ?: return@withContext false
            inputStream.use { input ->
                val outputStream = resolver.openOutputStream(destinationUri) ?: return@withContext false
                outputStream.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val bytesRead = input.read(buffer)
                        if (bytesRead < 0) {
                            break
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            coroutineContext.ensureActive()
            val publishedValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            published = resolver.update(destinationUri, publishedValues, null, null) == 1
            published
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        } finally {
            if (!published && galleryUri != null) {
                try {
                    resolver.delete(galleryUri, null, null)
                } catch (_: Exception) {
                    // 삭제할 수 없는 미완료 항목은 MediaStore의 만료 정책에 맡긴다.
                }
            }
        }
    }
