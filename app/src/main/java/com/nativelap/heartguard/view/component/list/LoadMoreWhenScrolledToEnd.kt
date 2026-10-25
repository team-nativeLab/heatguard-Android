package com.nativelap.heartguard.view.component.list

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import kotlinx.coroutines.flow.distinctUntilChanged

/** 일반 스크롤 Column이 끝에서 [prefetchDistance] 안쪽까지 내려오면 [onLoadMore]를 부른다.
 * 내용이 화면보다 짧아 스크롤할 수 없을 때도 끝에 닿은 것으로 본다. */
@Composable
fun LoadMoreWhenScrolledToEnd(
    scrollState: ScrollState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    prefetchDistance: Dp = HeartGuardComponentSize.ListLoadMorePrefetchDistance,
) {
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)
    val prefetchDistancePx = with(LocalDensity.current) { prefetchDistance.roundToPx() }
    LaunchedEffect(scrollState, canLoadMore, prefetchDistancePx) {
        if (!canLoadMore) {
            return@LaunchedEffect
        }
        snapshotFlow { scrollState.maxValue - scrollState.value <= prefetchDistancePx }
            .distinctUntilChanged()
            .collect { isNearEnd ->
                if (isNearEnd) {
                    latestOnLoadMore()
                }
            }
    }
}
