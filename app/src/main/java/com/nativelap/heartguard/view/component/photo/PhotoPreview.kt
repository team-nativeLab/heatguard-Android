package com.nativelap.heartguard.view.component.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 촬영 전후의 작업·휴식 사진을 동일한 비율의 미리보기로 보여준다. */
@Composable
fun PhotoPreview(
    photoPainter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Image(
            painter = photoPainter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PhotoPreviewPreview() {
    HeartGuardTheme {
        PhotoPreview(
            photoPainter = androidx.compose.ui.res.painterResource(
                com.nativelap.heartguard.R.drawable.record_work_photo,
            ),
            contentDescription = "촬영된 사진",
        )
    }
}
