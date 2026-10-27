package com.nativelap.heartguard.view.screen.inquiry

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.BottomActionBar
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.temperature.RecordSaveButton
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryUiState

@Composable
fun InquiryScreen(
    uiState: InquiryUiState,
    onEvent: (InquiryScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        bottomBar = {
            BottomActionBar {
                RecordSaveButton(
                    title =
                        stringResource(
                            if (uiState.submission ==
                                null
                            ) {
                                R.string.inquiry_submit
                            } else {
                                R.string.inquiry_done
                            },
                        ),
                    onClick = {
                        if (uiState.submission == null) {
                            onEvent(InquiryScreenEvent.SubmitClicked)
                        } else {
                            onEvent(InquiryScreenEvent.BackClicked)
                        }
                    },
                    enabled =
                        uiState.submission != null ||
                            (!uiState.isSubmitting && uiState.title.isNotBlank() && uiState.content.isNotBlank()),
                )
            }
        },
    ) { innerPadding ->
        ResponsivePageContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.inquiry_register_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(InquiryScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
                        .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
            ) {
                Text(
                    text =
                        stringResource(R.string.inquiry_register_description) + "\n" +
                            stringResource(R.string.inquiry_reply_note),
                    color = MaterialTheme.extraColors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
                InquiryInputField(
                    label = stringResource(R.string.inquiry_subject),
                    placeholder = stringResource(R.string.inquiry_subject_placeholder),
                    value = uiState.title,
                    onValueChange = { onEvent(InquiryScreenEvent.TitleChanged(it)) },
                    enabled = !uiState.isSubmitting,
                )
                InquiryInputField(
                    label = stringResource(R.string.inquiry_content),
                    placeholder = stringResource(R.string.inquiry_content_placeholder),
                    value = uiState.content,
                    onValueChange = { onEvent(InquiryScreenEvent.ContentChanged(it)) },
                    minHeight = HeartGuardComponentSize.InquiryBodyFieldHeight,
                    minLines = 6,
                    characterCount = uiState.content.codePointCount(0, uiState.content.length),
                    enabled = !uiState.isSubmitting,
                )
                if (uiState.isSubmitting) {
                    Text(stringResource(R.string.inquiry_submitting), color = MaterialTheme.extraColors.secondaryText)
                }
                if (uiState.hasError) {
                    Text(stringResource(R.string.inquiry_submit_error), color = MaterialTheme.colorScheme.error)
                }
                uiState.submission?.let { submission ->
                    Text(
                        text =
                            stringResource(R.string.inquiry_submit_success) + "\n" +
                                stringResource(R.string.inquiry_submit_receipt, submission.inquiryId),
                        color = MaterialTheme.extraColors.strongText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun InquiryInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = HeartGuardComponentSize.TextFieldHeight,
    minLines: Int = 1,
    characterCount: Int? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        val fieldShape = RoundedCornerShape(HeartGuardRadius.Card)
        val fieldColors =
            TextFieldDefaults.colors(
                focusedContainerColor =
                    if (characterCount == null) {
                        MaterialTheme.extraColors.inputFieldBackground
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                unfocusedContainerColor =
                    if (characterCount == null) {
                        MaterialTheme.extraColors.inputFieldBackground
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        if (characterCount == null) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = HeartGuardSpacing.Compact)
                        .semantics { contentDescription = label }
                        .heightIn(min = minHeight),
                placeholder = { Text(placeholder) },
                minLines = minLines,
                singleLine = minLines == 1,
                shape = fieldShape,
                colors = fieldColors,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = HeartGuardSpacing.Compact)
                        .heightIn(min = minHeight, max = minHeight)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.extraColors.cardBorder,
                            shape = fieldShape,
                        ),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .semantics { contentDescription = label },
                        placeholder = { Text(placeholder) },
                        minLines = minLines,
                        singleLine = false,
                        shape = fieldShape,
                        colors = fieldColors,
                    )
                    Text(
                        text = stringResource(R.string.inquiry_content_limit, characterCount),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = HeartGuardSpacing.Card,
                                    vertical = HeartGuardSpacing.Compact,
                                ),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.extraColors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun InquiryScreenPreview() {
    HeartGuardTheme { InquiryScreen(uiState = InquiryUiState(), onEvent = {}) }
}
