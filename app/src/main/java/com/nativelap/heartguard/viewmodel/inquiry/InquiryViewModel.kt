package com.nativelap.heartguard.viewmodel.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.usecase.GetInquiriesUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiryViewModel
    @Inject
    constructor(
        private val submitInquiryUseCase: SubmitInquiryUseCase,
        private val getInquiriesUseCase: GetInquiriesUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(InquiryUiState())
        val uiState: StateFlow<InquiryUiState> = mutableUiState.asStateFlow()

        private var submitInquiryJob: Job? = null
        private var loadInquiriesJob: Job? = null

        // 현재 목록에서 이미 받은 커서다. 서버 커서가 되돌아오면 무한 반복 대신 목록을 끝내고 실패를 알린다.
        private val loadedCursors = mutableSetOf<String>()

        init {
            // Activity 수명 ViewModel이라 로그아웃 뒤 다른 작업자가 로그인해도 이전 입력·접수 결과가 남지 않게 한다.
            clearStateWhenSessionEnds(sessionManager) {
                submitInquiryJob?.cancel()
                submitInquiryJob = null
                loadInquiriesJob?.cancel()
                loadInquiriesJob = null
                loadedCursors.clear()
                mutableUiState.value = InquiryUiState()
            }
        }

        /** 화면에 들어올 때 호출한다. 이전 접수 결과·오류 안내를 지우고 내 문의 목록을 새로 조회한다(작성 중인 입력은 유지). */
        fun openInquiry() {
            mutableUiState.value =
                mutableUiState.value.copy(
                    submission = null,
                    hasError = false,
                )
            loadInquiries()
        }

        /** 내 문의 목록 첫 페이지를 조회한다. 오류 카드의 재시도와 등록 성공 후 갱신에서도 쓴다. */
        fun loadInquiries() {
            loadInquiriesJob?.cancel()
            loadedCursors.clear()
            mutableUiState.value =
                mutableUiState.value.copy(
                    listState = InquiryListState.Loading,
                )
            requestInquiryPage(
                cursor = null,
                previousInquiries = null,
            )
        }

        /** 목록 끝에 닿았을 때 다음 문의를 이어 받는다. 이어 받기 실패 뒤의 재시도에서도 쓴다. */
        fun loadMoreInquiries() {
            val loadedState = mutableUiState.value.listState as? InquiryListState.Loaded ?: return
            val nextCursor = loadedState.nextCursor ?: return
            if (loadedState.isLoadingMore) {
                return
            }
            mutableUiState.value =
                mutableUiState.value.copy(
                    listState =
                        loadedState.copy(
                            isLoadingMore = true,
                            hasLoadMoreError = false,
                        ),
                )
            requestInquiryPage(
                cursor = nextCursor,
                previousInquiries = loadedState.inquiries,
            )
        }

        private fun requestInquiryPage(
            cursor: String?,
            previousInquiries: List<InquirySummary>?,
        ) {
            val owningGeneration = sessionManager.getSnapshot().generation
            loadInquiriesJob =
                viewModelScope.launch {
                    val pageResult = getInquiriesUseCase(cursor)
                    // 응답을 기다리는 동안 계정이 바뀌었으면 이전 계정 문의를 섞지 않는다.
                    if (owningGeneration != sessionManager.getSnapshot().generation) {
                        return@launch
                    }
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            listState =
                                when (pageResult) {
                                    is ApiResult.Success -> {
                                        cursor?.let(loadedCursors::add)
                                        val receivedCursor = pageResult.value.nextCursor
                                        val isRepeatedCursor = receivedCursor != null && receivedCursor in loadedCursors
                                        InquiryListState.Loaded(
                                            inquiries =
                                                (previousInquiries.orEmpty() + pageResult.value.inquiries)
                                                    .distinctBy { inquirySummary -> inquirySummary.inquiryId },
                                            nextCursor = receivedCursor.takeUnless { isRepeatedCursor },
                                            hasLoadMoreError = isRepeatedCursor,
                                        )
                                    }

                                    is ApiResult.Failure -> {
                                        val loadedState = mutableUiState.value.listState as? InquiryListState.Loaded
                                        if (previousInquiries == null || loadedState == null) {
                                            InquiryListState.Failed
                                        } else {
                                            loadedState.copy(
                                                isLoadingMore = false,
                                                hasLoadMoreError = true,
                                            )
                                        }
                                    }
                                },
                        )
                }
        }

        /** 제목 입력을 현재 화면 상태에 반영하고 이전 요청 결과를 지운다. */
        fun updateTitle(title: String) {
            mutableUiState.value =
                mutableUiState.value.copy(
                    title = title,
                    submission = null,
                    hasError = false,
                )
        }

        /** 문의 내용 입력을 현재 화면 상태에 반영하고 이전 요청 결과를 지운다. */
        fun updateContent(content: String) {
            mutableUiState.value =
                mutableUiState.value.copy(
                    content = content,
                    submission = null,
                    hasError = false,
                )
        }

        /** 제목과 내용을 서버에 등록하고 접수 결과 또는 재시도 가능한 오류 상태를 반영한다. */
        fun submitInquiry() {
            val currentState = mutableUiState.value
            if (currentState.isSubmitting || currentState.title.isBlank() || currentState.content.isBlank()) {
                return
            }

            mutableUiState.value =
                currentState.copy(
                    isSubmitting = true,
                    submission = null,
                    hasError = false,
                )
            submitInquiryJob =
                viewModelScope.launch {
                    when (
                        val submitResult =
                            submitInquiryUseCase(
                                title = currentState.title,
                                content = currentState.content,
                            )
                    ) {
                        is ApiResult.Success -> {
                            // 등록이 끝나면 입력을 비우고, 새 문의가 목록 맨 위에 보이도록 목록을 다시 조회한다.
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    title = "",
                                    content = "",
                                    isSubmitting = false,
                                    submission = submitResult.value,
                                )
                            loadInquiries()
                        }

                        is ApiResult.Failure -> {
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    isSubmitting = false,
                                    hasError = true,
                                )
                        }
                    }
                }
        }
    }
