package com.nativelap.heartguard.view.screen.inquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.LoadErrorCard
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.inquiry.InquiryListCard
import com.nativelap.heartguard.view.component.list.LoadMoreWhenScrolledToEnd
import com.nativelap.heartguard.view.component.list.PagedListFooter
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.inquiry.InquiryListState
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryUiState

/** Figma 25_문의하기 화면이다. 문의 입력·등록과 내 문의 목록(서버 조회, 최신순)을 보여준다. 문의 상세는 범위 밖이라 목록 항목은 눌리지 않는다. */
@Composable
fun InquiryScreen(
    uiState: InquiryUiState,
    onEvent: (InquiryScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val loadedListState = uiState.listState as? InquiryListState.Loaded
    LoadMoreWhenScrolledToEnd(
        scrollState = scrollState,
        canLoadMore =
            loadedListState != null &&
                loadedListState.nextCursor != null &&
                !loadedListState.isLoadingMore &&
                !loadedListState.hasLoadMoreError,
        onLoadMore = { onEvent(InquiryScreenEvent.LoadMore) },
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
    ) { innerPadding ->
        ResponsivePageContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.inquiry_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(InquiryScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                        .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                InquiryInputField(
                    label = stringResource(R.string.inquiry_subject),
                    placeholder = stringResource(R.string.inquiry_subject_placeholder),
                    value = uiState.title,
                    onValueChange = { onEvent(InquiryScreenEvent.TitleChanged(it)) },
                )
                InquiryInputField(
                    label = stringResource(R.string.inquiry_content),
                    placeholder = stringResource(R.string.inquiry_content_placeholder),
                    minHeight = HeartGuardComponentSize.InquiryBodyFieldHeight,
                    value = uiState.content,
                    onValueChange = { onEvent(InquiryScreenEvent.ContentChanged(it)) },
                    minLines = 5,
                )
                Text(
                    text = stringResource(R.string.inquiry_reply_note),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
                RecordSaveButton(
                    title = stringResource(R.string.inquiry_submit),
                    onClick = { onEvent(InquiryScreenEvent.SubmitClicked) },
                    enabled =
                        !uiState.isSubmitting &&
                            uiState.title.isNotBlank() &&
                            uiState.content.isNotBlank(),
                )
                if (uiState.isSubmitting) {
                    Text(
                        text = stringResource(R.string.inquiry_submitting),
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (uiState.hasError) {
                    Text(
                        text = stringResource(R.string.inquiry_submit_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                uiState.submission?.let { submission ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(HeartGuardRadius.Card),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(
                            modifier = Modifier.padding(HeartGuardSpacing.Item),
                            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
                        ) {
                            Text(
                                text = stringResource(R.string.inquiry_submit_success),
                                color = MaterialTheme.extraColors.strongText,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = stringResource(R.string.inquiry_submit_receipt, submission.inquiryId),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.inquiry_my_list),
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.extraColors.strongText,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    val listState = uiState.listState
                    if (listState is InquiryListState.Loaded) {
                        Text(
                            // 아직 받을 문의가 남았으면 '+'를 붙여 전체 건수가 아님을 드러낸다.
                            text =
                                if (listState.nextCursor != null) {
                                    stringResource(R.string.list_partial_count_format, listState.inquiries.size)
                                } else {
                                    stringResource(R.string.inquiry_list_count_format, listState.inquiries.size)
                                },
                            color = MaterialTheme.extraColors.secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                when (val listState = uiState.listState) {
                    InquiryListState.Loading -> {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = HeartGuardSpacing.Section),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    InquiryListState.Failed -> {
                        LoadErrorCard(
                            title = stringResource(R.string.inquiry_list_load_failure_title),
                            description = stringResource(R.string.inquiry_list_load_failure_description),
                            onRetryClick = { onEvent(InquiryScreenEvent.RetryListClicked) },
                        )
                    }

                    is InquiryListState.Loaded -> {
                        if (listState.inquiries.isEmpty()) {
                            Text(
                                text = stringResource(R.string.inquiry_list_empty),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = HeartGuardSpacing.Section),
                                color = MaterialTheme.extraColors.secondaryText,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            InquiryListCard(inquiries = listState.inquiries)
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

@Composable
private fun InquiryInputField(
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = HeartGuardComponentSize.TextFieldHeight,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Compact)
                    .heightIn(min = minHeight),
            placeholder = {
                Text(placeholder)
            },
            minLines = minLines,
            shape = RoundedCornerShape(HeartGuardRadius.InputBox),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                    unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.extraColors.authInputBackground,
                ),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = minLines == 1,
            maxLines = if (minLines == 1) 1 else Int.MAX_VALUE,
            label = null,
            supportingText = null,
            enabled = true,
            readOnly = false,
            isError = false,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun InquiryScreenLoadingPreview() {
    HeartGuardTheme {
        InquiryScreen(uiState = InquiryUiState(), onEvent = {})
    }
}
