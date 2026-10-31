package com.nativelap.heartguard.view.route.inquiry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.view.screen.inquiry.InquiryListScreen
import com.nativelap.heartguard.view.screen.inquiry.InquiryScreen
import com.nativelap.heartguard.viewmodel.inquiry.InquiryEffect
import com.nativelap.heartguard.viewmodel.inquiry.InquiryScreenEvent
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel

/** 문의하기 화면에 들어올 때마다 내 문의 목록을 새로 조회하고, 입력·등록·재시도 이벤트를 ViewModel에 전달한다. */
@Composable
internal fun HeartGuardInquiryRoute(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    inquiryViewModel: InquiryViewModel = hiltViewModel(),
) {
    val uiState by inquiryViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(inquiryViewModel) {
        inquiryViewModel.openInquiry()
        onPauseOrDispose {}
    }

    InquiryListScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                InquiryScreenEvent.BackClicked -> onBackClick()

                InquiryScreenEvent.RegisterClicked -> onRegisterClick()

                is InquiryScreenEvent.TitleChanged -> inquiryViewModel.updateTitle(event.title)

                is InquiryScreenEvent.ContentChanged -> inquiryViewModel.updateContent(event.content)

                InquiryScreenEvent.SubmitClicked,
                -> Unit

                InquiryScreenEvent.RetryListClicked -> inquiryViewModel.loadInquiries()

                InquiryScreenEvent.LoadMore -> inquiryViewModel.loadMoreInquiries()
            }
        },
    )
}

@Composable
internal fun HeartGuardInquiryRegistrationRoute(
    onBackClick: () -> Unit,
    inquiryViewModel: InquiryViewModel,
) {
    val uiState by inquiryViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(inquiryViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            inquiryViewModel.effects.collect { effect ->
                when (effect) {
                    InquiryEffect.NavigateToList -> onBackClick()
                }
            }
        }
    }

    InquiryScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                InquiryScreenEvent.BackClicked -> onBackClick()

                is InquiryScreenEvent.TitleChanged -> inquiryViewModel.updateTitle(event.title)

                is InquiryScreenEvent.ContentChanged -> inquiryViewModel.updateContent(event.content)

                InquiryScreenEvent.SubmitClicked -> inquiryViewModel.submitInquiry()

                InquiryScreenEvent.RegisterClicked,
                InquiryScreenEvent.RetryListClicked,
                InquiryScreenEvent.LoadMore,
                -> Unit
            }
        },
    )
}
