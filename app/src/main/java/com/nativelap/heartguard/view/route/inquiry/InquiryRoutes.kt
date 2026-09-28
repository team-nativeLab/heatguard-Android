package com.nativelap.heartguard.view.route.inquiry

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.inquiry.InquiryScreen
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel

@Composable
internal fun HeartGuardInquiryRoute(
    onBackClick: () -> Unit,
    inquiryViewModel: InquiryViewModel = hiltViewModel(),
) {
    val uiState by inquiryViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val submittedMessage = stringResource(R.string.inquiry_submit_success)

    LaunchedEffect(uiState.submittedInquiry?.inquiryId) {
        if (uiState.submittedInquiry != null) {
            snackbarHostState.showSnackbar(message = submittedMessage)
            inquiryViewModel.clearSubmissionMessage()
        }
    }

    InquiryScreen(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onTitleChanged = inquiryViewModel::onTitleChanged,
        onContentChanged = inquiryViewModel::onContentChanged,
        onSubmitClick = inquiryViewModel::submit,
    )
}
