package com.nativelap.heartguard.view.route.inquiry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.screen.inquiry.InquiryScreen
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel

/** 서버 문의 계약이 없는 상태를 화면에 전달하고, 뒤로 가기만 Navigation에 위임한다. */
@Composable
internal fun HeartGuardInquiryRoute(
    onBackClick: () -> Unit,
    inquiryViewModel: InquiryViewModel = hiltViewModel(),
) {
    val uiState by inquiryViewModel.uiState.collectAsStateWithLifecycle()

    InquiryScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                InquiryScreenEvent.BackClicked -> onBackClick()
                is InquiryScreenEvent.TitleChanged -> inquiryViewModel.updateTitle(event.title)
                is InquiryScreenEvent.ContentChanged -> inquiryViewModel.updateContent(event.content)
                InquiryScreenEvent.SubmitClicked -> inquiryViewModel.submitInquiry()
            }
        },
    )
}
