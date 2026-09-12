package com.nativelap.heartguard.view.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme

/** 헤더 양쪽의 아이콘을 동일한 터치 영역과 접근성 설명으로 노출한다. */
@Composable
fun HeartGuardHeaderActionButton(
    iconPainter: Painter,
    iconContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(HeartGuardComponentSize.TouchTarget)
            .semantics(mergeDescendants = true) {
                contentDescription = iconContentDescription
            },
    ) {
        Image(
            painter = iconPainter,
            contentDescription = null,
            modifier = Modifier.size(HeartGuardIconSize.HeaderAction),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartGuardHeaderActionButtonPreview() {
    HeartGuardTheme {
        HeartGuardHeaderActionButton(
            iconPainter = painterResource(R.drawable.menu_hamburger),
            iconContentDescription = stringResource(R.string.common_menu),
            onClick = {},
        )
    }
}
