package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** Figma처럼 화면 하단에 고정되는 주요 버튼 영역이다. Scaffold의 bottomBar로 쓴다.
 * 내비게이션 바와 키보드 중 더 큰 쪽만큼 띄워, 입력 중에도 버튼이 키보드에 가려지지 않게 한다. */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = HeartGuardComponentSize.PageContentMaxWidth)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(
                    horizontal = HeartGuardSpacing.BottomActionHorizontal,
                    vertical = HeartGuardSpacing.Item,
                ),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            content = content,
        )
    }
}
