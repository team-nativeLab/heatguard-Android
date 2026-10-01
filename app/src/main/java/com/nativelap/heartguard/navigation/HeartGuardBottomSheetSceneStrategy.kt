@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nativelap.heartguard.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur

/** 바텀시트 목적지(기록 유형 선택·저장 결과·긴급 호출)를 Material 3 ModalBottomSheet overlay로 표시한다. */
internal class HeartGuardBottomSheetSceneStrategy : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(
        entries: List<NavEntry<NavKey>>,
    ): Scene<NavKey>? {
        val lastEntry = entries.lastOrNull() ?: return null

        val isDismissible = lastEntry.metadata.get(BottomSheetKey)
        if (isDismissible == null || entries.size <= 1) {
            return null
        }

        return HeartGuardBottomSheetScene(
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            isDismissible = isDismissible,
            onDismissAttempt = lastEntry.metadata.get(DismissAttemptKey),
            onBack = onBack,
        )
    }

    companion object {
        /** 바텀시트 목적지를 entry metadata로 표시하기 위한 Navigation 3 키이다. 값은 스와이프·바깥 탭·뒤로가기로 닫을 수 있는지다. */
        object BottomSheetKey : NavMetadataKey<Boolean>

        object DismissAttemptKey : NavMetadataKey<() -> Unit>

        /** 특정 entry를 이 프로젝트의 ModalBottomSheet overlay로 렌더링하도록 표시한다.
         * [isDismissible]이 false면 일반적으로 닫히지 않으며, [onDismissAttempt]를 지정하면 닫기 제스처를
         * 소비하면서 해당 목적지의 상태 변경을 요청한다. */
        fun bottomSheet(
            isDismissible: Boolean = true,
            onDismissAttempt: (() -> Unit)? = null,
        ): Map<String, Any> {
            return metadata {
                put(BottomSheetKey, isDismissible)
                if (onDismissAttempt != null) {
                    put(DismissAttemptKey, onDismissAttempt)
                }
            }
        }
    }
}

/** Navigation 3 back stack 수명과 바텀시트 닫힘 애니메이션을 연결하는 overlay scene이다. */
private data class HeartGuardBottomSheetScene(
    private val entry: NavEntry<NavKey>,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val isDismissible: Boolean,
    private val onDismissAttempt: (() -> Unit)?,
    private val onBack: () -> Unit,
) : OverlayScene<NavKey> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<NavKey>> = previousEntries
    private var sheetState: SheetState? = null
    private var isBeingRemoved = false

    override val content: @Composable () -> Unit = {
        // 닫을 수 없는 시트는 사용자 드래그로 Hidden이 되지 않게 막는다. back stack 제거 시의 hide()는 그대로 동작한다.
        val currentSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue ->
                if (targetValue != SheetValue.Hidden || isBeingRemoved || isDismissible) {
                    true
                } else {
                    onDismissAttempt?.invoke()
                    false
                }
            },
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
            onDismissRequest = {
                if (isDismissible) {
                    onBack()
                } else {
                    onDismissAttempt?.invoke()
                }
            },
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = isDismissible,
            ),
            sheetState = currentSheetState,
            shape = RectangleShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            dragHandle = null,
            contentWindowInsets = {
                WindowInsets(0)
            },
        ) {
            // inset을 비워 둔 투명 시트라, 큰 글꼴·작은 화면에서 내용이 길면 상태 표시줄 밑까지 올라간다.
            // 위쪽 패딩 대신 최대 높이를 상태 표시줄 아래로 제한해 스크림 없는 띠 없이 제목이 가려지지 않게 한다.
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            BoxWithConstraints {
                Box(modifier = Modifier.heightIn(max = maxHeight - statusBarTop)) {
                    entry.Content()
                }
            }
        }
    }

    /** back stack entry가 제거되기 전에 닫힘 애니메이션을 완료한다. */
    override suspend fun onRemove() {
        isBeingRemoved = true
        sheetState?.hide()
    }
}
