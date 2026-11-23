package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary

/** 문의 화면 상태다. */
data class InquiryUiState(
    val title: String = "",
    val content: String = "",
    val isSubmitting: Boolean = false,
    val submissionError: InquiryError? = null,
    val requiresListCheckBeforeResubmission: Boolean = false,
    val isSubmissionGuardReady: Boolean = false,
    val submissionCheckResult: InquirySubmissionCheckResult? = null,
    val listState: InquiryListState = InquiryListState.Loading,
) {
    fun canSubmitInquiry(): Boolean =
        !isSubmitting &&
            !requiresListCheckBeforeResubmission &&
            isSubmissionGuardReady &&
            submissionError != InquiryError.LoginRequired &&
            listState is InquiryListState.Loaded &&
            title.isNotBlank() &&
            content.isNotBlank()

    fun afterSubmissionFailure(
        error: InquiryError,
        requiresInquiryListCheck: Boolean,
    ): InquiryUiState =
        copy(
            isSubmitting = false,
            submissionError = error,
            requiresListCheckBeforeResubmission = requiresInquiryListCheck,
            submissionCheckResult = null,
        )

    fun afterInquiryListVerification(
        found: Boolean,
        attemptedTitle: String,
        attemptedContent: String,
    ): InquiryUiState {
        if (!found) return this
        val shouldPreserveDraft = title != attemptedTitle || content != attemptedContent
        return copy(
            title = if (shouldPreserveDraft) title else "",
            content = if (shouldPreserveDraft) content else "",
            submissionError = null,
            requiresListCheckBeforeResubmission = false,
            submissionCheckResult = InquirySubmissionCheckResult.Found,
        )
    }

    fun afterSubmissionGuardConfirmation(
        submittedDraftIsUnchanged: Boolean,
        titleAtCheckStart: String,
        contentAtCheckStart: String,
    ): InquiryUiState {
        val draftStillMatchesCheckSnapshot =
            title == titleAtCheckStart && content == contentAtCheckStart
        val shouldClearDraft = submittedDraftIsUnchanged && draftStillMatchesCheckSnapshot
        return copy(
            title = if (shouldClearDraft) "" else title,
            content = if (shouldClearDraft) "" else content,
            submissionError = null,
            requiresListCheckBeforeResubmission = false,
            submissionCheckResult = InquirySubmissionCheckResult.Found,
        )
    }
}

/** 문의 요청 실패를 UI 문구에 대응시키는 분류다. */
enum class InquiryError {
    LoginRequired,
    Network,
    RequestRejected,
    AccessDenied,
    Server,
    UnreadableResponse,
    ListCheckRequired,
    Unknown,
    LocalStorage,
}

enum class InquirySubmissionCheckResult {
    Found,
}

internal fun ApiError.toInquiryError(isTeamMemberSessionActive: Boolean): InquiryError =
    when (this) {
        is ApiError.Http -> {
            when {
                statusCode == HTTP_UNAUTHORIZED -> InquiryError.LoginRequired
                statusCode == HTTP_FORBIDDEN -> InquiryError.AccessDenied
                statusCode in HTTP_BAD_REQUEST..HTTP_CLIENT_ERROR_MAX -> InquiryError.RequestRejected
                statusCode >= HTTP_SERVER_ERROR -> InquiryError.Server
                else -> InquiryError.Unknown
            }
        }

        ApiError.Network -> {
            InquiryError.Network
        }

        ApiError.SessionChanged -> {
            if (isTeamMemberSessionActive) {
                InquiryError.Unknown
            } else {
                InquiryError.LoginRequired
            }
        }

        ApiError.SiteNotSelected,
        ApiError.TeamNotSelected,
        -> {
            InquiryError.AccessDenied
        }

        ApiError.Serialization -> {
            InquiryError.UnreadableResponse
        }

        is ApiError.ServerRejected -> {
            InquiryError.RequestRejected
        }

        ApiError.LocalStorage,
        ApiError.Unknown,
        -> {
            if (this == ApiError.LocalStorage) InquiryError.LocalStorage else InquiryError.Unknown
        }
    }

internal fun ApiError.requiresInquiryListCheck(): Boolean =
    when (this) {
        ApiError.Network,
        ApiError.Serialization,
        ApiError.Unknown,
        -> true

        is ApiError.Http -> statusCode >= HTTP_SERVER_ERROR

        is ApiError.ServerRejected -> errorCode == SERVICE_DEGRADED_ERROR_CODE

        ApiError.SessionChanged,
        ApiError.SiteNotSelected,
        ApiError.TeamNotSelected,
        ApiError.LocalStorage,
        -> false
    }

internal fun InquiryListState.afterLoadMoreFailure(error: InquiryError): InquiryListState =
    when (this) {
        is InquiryListState.Loaded -> {
            copy(
                isLoadingMore = false,
                loadMoreError = error,
            )
        }

        InquiryListState.Loading,
        is InquiryListState.Failed,
        -> {
            this
        }
    }

/** 내 문의 목록 조회 결과다. */
sealed interface InquiryListState {
    data object Loading : InquiryListState

    /** [nextCursor]가 null이면 마지막 페이지다. 이어 받기 오류가 나도 이미 받은 문의는 유지한다. */
    data class Loaded(
        val inquiries: List<InquirySummary>,
        val nextCursor: String? = null,
        val isLoadingMore: Boolean = false,
        val loadMoreError: InquiryError? = null,
        val sessionGeneration: Long? = null,
    ) : InquiryListState {
        val hasLoadMoreError: Boolean get() = loadMoreError != null

        fun belongsToSession(generation: Long): Boolean = sessionGeneration == generation
    }

    data class Failed(
        val error: InquiryError,
    ) : InquiryListState
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_CLIENT_ERROR_MAX = 499
private const val HTTP_SERVER_ERROR = 500
private const val SERVICE_DEGRADED_ERROR_CODE = "SERVICE_DEGRADED"
