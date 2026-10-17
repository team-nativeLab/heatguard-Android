package com.nativelap.heartguard.view.route.inquiry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.screen.inquiry.InquiryScreen
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel

/** 문의하기 화면에 들어올 때마다 내 문의 목록을 새로 조회하고, 입력·등록·재시도 이벤트를 ViewModel에 전달한다. */
@Composable
internal fun HeartGuardInquiryRoute(
    onBackClick: () -> Unit,
    inquiryViewModel: InquiryViewModel = hiltViewModel(),
) {
    val uiState by inquiryViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(inquiryViewModel) {
        inquiryViewModel.openInquiry()
    }

    InquiryScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                InquiryScreenEvent.BackClicked -> onBackClick()
                is InquiryScreenEvent.TitleChanged -> inquiryViewModel.updateTitle(event.title)
                is InquiryScreenEvent.ContentChanged -> inquiryViewModel.updateContent(event.content)
                InquiryScreenEvent.SubmitClicked -> inquiryViewModel.submitInquiry()
                InquiryScreenEvent.RetryListClicked -> inquiryViewModel.loadInquiries()
                InquiryScreenEvent.LoadMore -> inquiryViewModel.loadMoreInquiries()
            }
        },
    )
}
