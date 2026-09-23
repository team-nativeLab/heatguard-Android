package com.nativelap.heartguard.view.component.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 선택 사진을 2열 미리보기와 개별 삭제 동작으로 표현한다. */
@Composable
internal fun SelectedPhotoGrid(
    photoUris: List<Uri>,
    onRemovePhoto: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (photoUris.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        photoUris.chunked(PHOTOS_PER_ROW).forEachIndexed { rowIndex, rowUris ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                rowUris.forEachIndexed { columnIndex, uri ->
                    SelectedPhotoItem(
                        uri = uri,
                        onRemoveClick = { onRemovePhoto(uri) },
                        modifier = Modifier.weight(1f),
                        index = rowIndex * PHOTOS_PER_ROW + columnIndex + 1,
                    )
                }

                if (rowUris.size < PHOTOS_PER_ROW) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SelectedPhotoItem(
    uri: Uri,
    onRemoveClick: () -> Unit,
    modifier: Modifier,
    index: Int,
) {
    val context = LocalContext.current
    val photoBitmap = produceState<Bitmap?>(
        initialValue = null,
        key1 = uri,
    ) {
        value = try {
            withContext(Dispatchers.IO) {
                decodeSampledBitmap(uri, context.contentResolver::openInputStream)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            null
        }
    }.value

    val photoDescription = stringResource(R.string.photo_selected_accessibility, index)
    val removeDescription = stringResource(R.string.photo_remove_accessibility, index)

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = photoDescription },
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box {
            if (photoBitmap == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.photo_preview_unavailable),
                        modifier = Modifier.padding(top = HeartGuardSpacing.Tight),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .semantics { contentDescription = removeDescription },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun decodeSampledBitmap(
    uri: Uri,
    openInputStream: (Uri) -> java.io.InputStream?,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, bounds)
    } ?: return null

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_PREVIEW_DIMENSION ||
        bounds.outHeight / sampleSize > MAX_PREVIEW_DIMENSION
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    return openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, options)
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun SelectedPhotoGridEmptyPreview() {
    HeartGuardTheme {
        SelectedPhotoGrid(
            photoUris = emptyList(),
            onRemovePhoto = {},
        )
    }
}

private const val MAX_PREVIEW_DIMENSION = 1024
private const val PHOTOS_PER_ROW = 2
