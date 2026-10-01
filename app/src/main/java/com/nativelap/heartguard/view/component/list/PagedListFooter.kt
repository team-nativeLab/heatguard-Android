package com.nativelap.heartguard.view.component.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** LazyColumn 끝에 [PagedListFooter]를 붙인다. */
fun LazyListScope.pagedListFooter(
    keyPrefix: String,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
    canRetry: Boolean,
    onRetryClick: () -> Unit,
) {
    if (isLoadingMore || hasLoadMoreError) {
        item(key = "$keyPrefix-paged-list-footer") {
            PagedListFooter(
                isLoadingMore = isLoadingMore,
                hasLoadMoreError = hasLoadMoreError,
                canRetry = canRetry,
                onRetryClick = onRetryClick,
            )
        }
    }
}

/** 이어 받는 중이면 진행 표시를, 실패했으면 재시도 버튼(재시도할 수 없으면 안내 문구)을 목록 끝에 둔다. */
@Composable
fun PagedListFooter(
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
    canRetry: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoadingMore -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Item),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(HeartGuardComponentSize.ListLoadingIndicator))
        }

        hasLoadMoreError && canRetry -> TextButton(
            onClick = onRetryClick,
            modifier = modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.list_load_more_retry))
        }

        hasLoadMoreError -> Text(
            text = stringResource(R.string.list_load_more_failed),
            modifier = modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Item),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
