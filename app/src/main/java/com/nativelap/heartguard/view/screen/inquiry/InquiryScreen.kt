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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import com.nativelap.heartguard.viewmodel.inquiry.InquiryUiState

@Composable
fun InquiryScreen(
    state: InquiryUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.heartGuardResponsivePage(MaterialTheme.extraColors.pageBackground),
        containerColor = MaterialTheme.extraColors.pageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = HeartGuardSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.extraColors.pageBackground,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "‹",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.inquiry_title),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HeartGuardSpacing.Section)
                .padding(top = HeartGuardSpacing.Section, bottom = HeartGuardSpacing.LargeSection),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Section),
        ) {
            InquiryInputField(
                label = stringResource(R.string.inquiry_subject_label),
                value = state.title,
                placeholder = stringResource(R.string.inquiry_subject_placeholder),
                onValueChange = onTitleChanged,
                singleLine = true,
            )

            Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
                InquiryInputField(
                    label = stringResource(R.string.inquiry_content_label),
                    value = state.content,
                    placeholder = stringResource(R.string.inquiry_content_placeholder),
                    onValueChange = onContentChanged,
                    singleLine = false,
                    modifier = Modifier.heightIn(min = 140.dp),
                )
                Text(
                    text = stringResource(R.string.inquiry_reply_hint),
                    color = MaterialTheme.extraColors.homeMutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            state.submissionError?.let {
                Text(
                    text = stringResource(R.string.inquiry_submit_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onSubmitClick,
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(HeartGuardRadius.Button),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(HeartGuardSpacing.Tight))
                }
                Text(text = stringResource(R.string.inquiry_submit_action))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Tight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.inquiry_list_title),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.inquiry_list_unavailable_count),
                    color = MaterialTheme.extraColors.homeMutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(HeartGuardSpacing.Item),
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Text(
                        text = stringResource(R.string.inquiry_list_unavailable_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = stringResource(R.string.inquiry_list_unavailable_description),
                        color = MaterialTheme.extraColors.homeMutedText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InquiryInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 48.dp else 140.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                unfocusedContainerColor = MaterialTheme.extraColors.authInputBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.extraColors.authInputBackground,
                focusedPlaceholderColor = MaterialTheme.extraColors.disabledText,
                unfocusedPlaceholderColor = MaterialTheme.extraColors.disabledText,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun InquiryScreenPreview() {
    HeartGuardTheme {
        InquiryScreen(
            state = InquiryUiState(),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onTitleChanged = {},
            onContentChanged = {},
            onSubmitClick = {},
        )
    }
}

@Preview(name = "Compact large text", showBackground = true, widthDp = 320, heightDp = 740, fontScale = 2f)
@Composable
private fun InquiryScreenCompactPreview() {
    HeartGuardTheme {
        InquiryScreen(
            state = InquiryUiState(),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onTitleChanged = {},
            onContentChanged = {},
            onSubmitClick = {},
        )
    }
}

@Preview(name = "Wide", showBackground = true, widthDp = 840, heightDp = 1024)
@Composable
private fun InquiryScreenWidePreview() {
    HeartGuardTheme {
        InquiryScreen(
            state = InquiryUiState(),
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onTitleChanged = {},
            onContentChanged = {},
            onSubmitClick = {},
        )
    }
}
