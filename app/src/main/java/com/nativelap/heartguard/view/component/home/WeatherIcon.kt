package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize

/** 현장 날씨 상태를 나타내는 Figma 아이콘 표시 영역이다. */
@Composable
fun WeatherIcon(
    weatherPainter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = weatherPainter,
        contentDescription = contentDescription,
        modifier = modifier.size(HeartGuardIconSize.Weather),
    )
}
