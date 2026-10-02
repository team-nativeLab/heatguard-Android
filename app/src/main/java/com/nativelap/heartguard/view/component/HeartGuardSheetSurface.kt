package com.nativelap.heartguard.view.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.extraColors

/** 바텀시트 목적지(저장 결과·긴급 호출)가 공통으로 쓰는 위쪽 모서리가 둥근 시트 배경이다. */
@Composable
fun HeartGuardSheetSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = HeartGuardRadius.Sheet,
            topEnd = HeartGuardRadius.Sheet,
        ),
        color = MaterialTheme.extraColors.pageBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = HeartGuardComponentSize.PageContentMaxWidth)
                    .fillMaxWidth(),
                content = content,
            )
        }
    }
}
