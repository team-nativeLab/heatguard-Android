package com.nativelap.heartguard.view.screen.inquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.LoadErrorCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.inquiry.InquiryListCard
import com.nativelap.heartguard.view.component.list.LoadMoreWhenNearEnd
import com.nativelap.heartguard.view.component.list.PagedListFooter
import com.nativelap.heartguard.viewmodel.inquiry.InquiryListState
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryUiState

@Composable
fun InquiryListScreen(
    uiState: InquiryUiState,
    onEvent: (InquiryScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listScrollState = rememberLazyListState()
    val loadedState = uiState.listState as? InquiryListState.Loaded
    LoadMoreWhenNearEnd(
        listState = listScrollState,
        canLoadMore = loadedState?.nextCursor != null && !loadedState.isLoadingMore && !loadedState.hasLoadMoreError,
        onLoadMore = { onEvent(InquiryScreenEvent.LoadMore) },
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                Button(
                    onClick = { onEvent(InquiryScreenEvent.RegisterClicked) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = HeartGuardComponentSize.PrimaryButtonHeight),
                    shape = RoundedCornerShape(HeartGuardRadius.PrimaryAction),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(
                        text = stringResource(R.string.inquiry_register),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        },
    ) { innerPadding ->
        ResponsivePageContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.inquiry_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(InquiryScreenEvent.BackClicked) },
            )
            LazyColumn(
                state = listScrollState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentPadding =
                    PaddingValues(
                        start = HeartGuardSpacing.AccountContentHorizontal,
                        end = HeartGuardSpacing.AccountContentHorizontal,
                        top = HeartGuardSpacing.Item,
                        bottom = HeartGuardSpacing.Section,
                    ),
            ) {
                item(key = "description") {
                    Text(
                        modifier = Modifier.padding(bottom = HeartGuardSpacing.Item),
                        text = stringResource(R.string.inquiry_list_description),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item(key = "heading") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = HeartGuardSpacing.Item),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.inquiry_my_list),
                            color = MaterialTheme.extraColors.strongText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        loadedState
                            ?.takeIf { state -> state.nextCursor == null && !state.hasLoadMoreError }
                            ?.let { state ->
                                Text(
                                    text = stringResource(R.string.inquiry_list_count_format, state.inquiries.size),
                                    color = MaterialTheme.extraColors.secondaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                    }
                }
                when (val listState = uiState.listState) {
                    InquiryListState.Loading -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = HeartGuardSpacing.Section),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }

                    InquiryListState.Failed -> {
                        item(key = "error") {
                            LoadErrorCard(
                                title = stringResource(R.string.inquiry_list_load_failure_title),
                                description = stringResource(R.string.inquiry_list_load_failure_description),
                                onRetryClick = { onEvent(InquiryScreenEvent.RetryListClicked) },
                            )
                        }
                    }

                    is InquiryListState.Loaded -> {
                        if (listState.inquiries.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(R.string.inquiry_list_empty),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = HeartGuardSpacing.Section),
                                    color = MaterialTheme.extraColors.secondaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = listState.inquiries,
                                key = { _, inquiry -> "inquiry-${inquiry.inquiryId}" },
                            ) { index, inquiry ->
                                InquiryListCard(
                                    inquiries = listOf(inquiry),
                                    isFirst = index == 0,
                                    isLast = index == listState.inquiries.lastIndex,
                                )
                            }
                            item(key = "footer") {
                                Box(Modifier.padding(top = HeartGuardSpacing.Item)) {
                                    PagedListFooter(
                                        isLoadingMore = listState.isLoadingMore,
                                        hasLoadMoreError = listState.hasLoadMoreError,
                                        canRetry = listState.nextCursor != null,
                                        onRetryClick = { onEvent(InquiryScreenEvent.LoadMore) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
