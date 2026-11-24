package com.nativelap.heartguard.view.screen.inquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
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
import com.nativelap.heartguard.viewmodel.inquiry.InquiryError
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
                uiState.submissionCheckResult?.let { checkResult ->
                    item(key = "submission-check-result") {
                        val message =
                            if (checkResult ==
                                com.nativelap.heartguard.viewmodel.inquiry.InquirySubmissionCheckResult.Found
                            ) {
                                R.string.inquiry_submission_found
                            } else {
                                R.string.inquiry_submission_not_found
                            }
                        Text(
                            text = stringResource(message),
                            modifier = Modifier.padding(bottom = HeartGuardSpacing.Item),
                            color = MaterialTheme.extraColors.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (uiState.requiresListCheckBeforeResubmission) {
                    item(key = "submission-check-required") {
                        Column {
                            Text(
                                text =
                                    stringResource(
                                        uiState.submissionError?.let(InquiryError::messageResource)
                                            ?: R.string.inquiry_submission_check_required,
                                    ),
                                modifier = Modifier.padding(bottom = HeartGuardSpacing.Compact),
                                color =
                                    if (uiState.submissionError == InquiryError.LocalStorage) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.extraColors.secondaryText
                                    },
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { onEvent(InquiryScreenEvent.VerifySubmissionClicked) }) {
                                Text(stringResource(R.string.inquiry_submission_check_action))
                            }
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

                    is InquiryListState.Failed -> {
                        item(key = "error") {
                            LoadErrorCard(
                                title = stringResource(R.string.inquiry_list_load_failure_title),
                                description = stringResource(listState.error.messageResource()),
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
                                Column(Modifier.padding(top = HeartGuardSpacing.Item)) {
                                    listState.loadMoreError?.let { error ->
                                        Text(
                                            text = stringResource(error.messageResource()),
                                            modifier = Modifier.padding(bottom = HeartGuardSpacing.Compact),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
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

private fun InquiryError.messageResource(): Int =
    when (this) {
        InquiryError.LoginRequired -> R.string.inquiry_login_required
        InquiryError.Network -> R.string.inquiry_network_error
        InquiryError.RequestRejected -> R.string.inquiry_request_rejected
        InquiryError.AccessDenied -> R.string.inquiry_access_denied
        InquiryError.Server -> R.string.inquiry_server_error
        InquiryError.UnreadableResponse -> R.string.inquiry_response_error
        InquiryError.ListCheckRequired -> R.string.inquiry_list_check_required
        InquiryError.LocalStorage -> R.string.inquiry_local_storage_error
        InquiryError.Unknown -> R.string.inquiry_unknown_error
    }
