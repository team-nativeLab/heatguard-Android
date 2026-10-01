package com.nativelap.heartguard.view.component.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 목록의 마지막 [prefetchItemCount]개 안쪽이 보이면 [onLoadMore]를 부른다.
 * [canLoadMore]가 바뀌면 다시 확인하므로, 짧은 목록이나 필터로 줄어든 목록도 화면을 채울 때까지 이어 받는다. */
@Composable
fun LoadMoreWhenNearEnd(
    listState: LazyListState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    prefetchItemCount: Int = DEFAULT_PREFETCH_ITEM_COUNT,
) {
    val latestOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState, canLoadMore, prefetchItemCount) {
        if (!canLoadMore) {
            return@LaunchedEffect
        }
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - prefetchItemCount
        }
            .distinctUntilChanged()
            .collect { isNearEnd ->
                if (isNearEnd) {
                    latestOnLoadMore()
                }
            }
    }
}

private const val DEFAULT_PREFETCH_ITEM_COUNT = 3
