@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nativelap.heartguard.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur

/** 기록 유형 선택 목적지를 Material 3 ModalBottomSheet overlay로 표시한다. */
internal class HeartGuardBottomSheetSceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val lastEntry = entries.lastOrNull() ?: return null

        if (
            lastEntry.metadata.get(BottomSheetKey) == null ||
                entries.size <= 1
        ) {
            return null
        }

        return HeartGuardBottomSheetScene(
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            onBack = onBack,
        )
    }

    companion object {
        /** 바텀시트 목적지를 entry metadata로 표시하기 위한 Navigation 3 키이다. */
        object BottomSheetKey : NavMetadataKey<Unit>

        /** 특정 entry를 이 프로젝트의 ModalBottomSheet overlay로 렌더링하도록 표시한다. */
        fun bottomSheet(): Map<String, Any> {
            return metadata {
                put(BottomSheetKey, Unit)
            }
        }
    }
}

/** Navigation 3 back stack 수명과 바텀시트 닫힘 애니메이션을 연결하는 overlay scene이다. */
private data class HeartGuardBottomSheetScene(
    private val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val onBack: () -> Unit,
) : OverlayScene<NavKey> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries
    private var sheetState: SheetState? = null

    override val content: @Composable () -> Unit = {
        val currentSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

        DisposableEffect(currentSheetState) {
            sheetState = currentSheetState

            onDispose {
                if (sheetState === currentSheetState) {
                    sheetState = null
                }
            }
        }

        ApplyDialogWindowBackgroundBlur(
            blurRadius = HeartGuardOverlayBlur.FigmaBackdrop,
        )

        // 시트의 모양·배경·여백은 각 목적지의 Sheet 컴포넌트가 그리므로, 여기서는 Material 기본 배경(라벤더)·
        // 드래그 핸들·모서리·그림자·inset을 모두 비워 두 겹으로 겹쳐 보이지 않게 한다.
        ModalBottomSheet(
            onDismissRequest = onBack,
            sheetState = currentSheetState,
            shape = RectangleShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = null,
            contentWindowInsets = {
                WindowInsets(0)
            },
        ) {
            entry.Content()
        }
    }

    /** back stack entry가 제거되기 전에 닫힘 애니메이션을 완료한다. */
    override suspend fun onRemove() {
        sheetState?.hide()
    }
}
