package com.nativelap.heartguard.view.component.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 로그인 화면에서 브랜드 이미지를 Figma의 표시 크기로 노출한다. */
@Composable
fun BrandMark(
    brandPainter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = brandPainter,
        contentDescription = contentDescription,
        modifier = modifier.size(HeartGuardIconSize.BrandMark),
    )
}

@Preview(showBackground = true)
@Composable
private fun BrandMarkPreview() {
    HeartGuardTheme {
        BrandMark(
            brandPainter = painterResource(R.drawable.heart_guard_logo),
            contentDescription = stringResource(R.string.brand_name),
        )
    }
}
