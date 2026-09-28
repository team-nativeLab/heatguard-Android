package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.SubcomposeAsyncImage
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 기록 사진 카드다(Figma 21 현장 사진·22 상단 사진). [photoUrls]는 상세 응답의 짧게 유효한 서명 URL이다.
 * 불러오는 동안 진행 표시를, 실패하거나 URL이 없으면 "사진을 불러올 수 없어요"를 보여준다. [title]이 null이면 제목 없이 사진만 둔다. */
@Composable
fun RecordDetailPhotoCard(
    photoUrls: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = MaterialTheme.extraColors.strongText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            if (photoUrls.isEmpty()) {
                PhotoFrame {
                    PhotoUnavailableContent()
                }
            }
            photoUrls.forEachIndexed { photoIndex, photoUrl ->
                PhotoFrame {
                    SubcomposeAsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(R.string.history_detail_photo_description, photoIndex + 1),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                PhotoUnavailableContent()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PHOTO_ASPECT_RATIO)
            .clip(RoundedCornerShape(HeartGuardRadius.InputBox))
            .background(MaterialTheme.extraColors.photoContainer),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PhotoUnavailableContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.extraColors.tertiaryText,
        )
        Text(
            text = stringResource(R.string.history_detail_photo_unavailable),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// Figma 21 현장 사진 영역(약 290x184) 비율이다.
private const val PHOTO_ASPECT_RATIO = 1.58f

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RecordDetailPhotoCardPreview() {
    HeartGuardTheme {
        RecordDetailPhotoCard(
            photoUrls = emptyList(),
            title = "현장 사진",
        )
    }
}
