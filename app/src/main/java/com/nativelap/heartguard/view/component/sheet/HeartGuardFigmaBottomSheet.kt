package com.nativelap.heartguard.view.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

/** Figma 바텀 시트 공용 래퍼다. Material 기본 핸들·배경·모서리 대신 Figma 값(상단 모서리 40dp, 그랩버, 옅은 배경, 딤 40%)을 쓰고 배경을 블러한다.
 * 좌우 24dp·하단 34dp 여백은 래퍼가 주므로 [content]에서는 다시 주지 않는다. 시트 높이는 상태 표시줄 아래로 제한한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartGuardFigmaBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceColor = MaterialTheme.extraColors.bottomSheetSurface
    val grabHandleColor = MaterialTheme.extraColors.bottomSheetGrabHandle

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        scrimColor = MaterialTheme.extraColors.overlayScrim,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = {
            WindowInsets(0)
        },
    ) {
        ApplyDialogWindowBackgroundBlur(
            blurRadius = HeartGuardOverlayBlur.FigmaBackdrop,
        )

        // 시스템 인셋을 비운 투명 시트라 큰 글꼴·작은 화면에서 내용이 상태 표시줄 밑까지 올라가지 않게 높이를 제한한다.
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // 내비게이션 바가 Figma 하단 여백보다 높으면 그 높이를 써서 버튼이 바 밑에 가려지지 않게 한다.
        val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val contentBottom = maxOf(HeartGuardSpacing.SheetBottom, navigationBarBottom)

        BoxWithConstraints {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight - statusBarTop)
                        .clip(
                            RoundedCornerShape(
                                topStart = HeartGuardRadius.Sheet,
                                topEnd = HeartGuardRadius.Sheet,
                            ),
                        ).background(surfaceColor),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = HeartGuardSpacing.SheetGrabHandleTop)
                            .size(
                                width = HeartGuardComponentSize.SheetGrabHandleWidth,
                                height = HeartGuardComponentSize.SheetGrabHandleHeight,
                            ).clip(RoundedCornerShape(HeartGuardRadius.Pill))
                            .background(grabHandleColor),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = HeartGuardSpacing.LargeSection,
                                end = HeartGuardSpacing.LargeSection,
                                bottom = contentBottom,
                            ),
                    content = content,
                )
            }
        }
    }
}
