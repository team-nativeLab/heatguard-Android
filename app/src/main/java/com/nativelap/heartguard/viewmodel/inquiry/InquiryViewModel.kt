package com.nativelap.heartguard.viewmodel.inquiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import com.nativelap.heartguard.domain.inquiry.usecase.GetInquiriesUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.InquirySubmissionGuardUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiryViewModel
    @Inject
    constructor(
        private val submitInquiryUseCase: SubmitInquiryUseCase,
        private val getInquiriesUseCase: GetInquiriesUseCase,
        private val submissionGuardUseCase: InquirySubmissionGuardUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(InquiryUiState())
        val uiState: StateFlow<InquiryUiState> = mutableUiState.asStateFlow()
        private val effectChannel = Channel<InquiryEffect>(Channel.BUFFERED)
        val effects: Flow<InquiryEffect> = effectChannel.receiveAsFlow()
        private var submitInquiryJob: Job? = null
        private var loadInquiriesJob: Job? = null
        private var loadedCursors = mutableSetOf<String>()
        private var activeGuardOwnerId: String? = null
        private var hasPendingSubmissionGuard = false
        private var guardRestoreJob: Job? = null
        private var observedSessionSnapshot: SessionSnapshot? = null

        init {
            clearStateWhenSessionEnds(sessionManager) {
                val currentSession = sessionManager.getSnapshot()
                val alreadyResetForCurrentSession =
                    hasObservedInquirySessionIdentity(observedSessionSnapshot, currentSession)
                if (!alreadyResetForCurrentSession) {
                    resetSessionScopedInquiryState()
                }
            }
            viewModelScope.launch {
                sessionManager.sessionSnapshot.collect { session ->
                    if (shouldResetInquiryStateForSessionTransition(observedSessionSnapshot, session)) {
                        resetSessionScopedInquiryState()
                    }
                    observedSessionSnapshot = session
                    val ownerId = session.sessionIdentifier
                    if (session.isTeamMemberSessionActive() && !ownerId.isNullOrBlank()) {
                        restoreSubmissionGuard(session.generation, ownerId)
                    }
                }
            }
        }

        private fun resetSessionScopedInquiryState() {
            val hasActiveJobs =
                submitInquiryJob?.isActive == true ||
                    loadInquiriesJob?.isActive == true ||
                    guardRestoreJob?.isActive == true
            val hasStateToReset =
                hasSessionScopedInquiryState(
                    hasActiveJobs = hasActiveJobs,
                    hasLoadedCursors = loadedCursors.isNotEmpty(),
                    hasGuardOwner = activeGuardOwnerId != null,
                    hasPendingSubmission = hasPendingSubmissionGuard,
                    uiStateIsDefault = mutableUiState.value == InquiryUiState(),
                )
            if (!hasStateToReset) return

            submitInquiryJob?.cancel()
            loadInquiriesJob?.cancel()
            guardRestoreJob?.cancel()
            submitInquiryJob = null
            loadInquiriesJob = null
            guardRestoreJob = null
            loadedCursors.clear()
            activeGuardOwnerId = null
            hasPendingSubmissionGuard = false
            mutableUiState.value = InquiryUiState()
        }

        fun openInquiry() {
            loadInquiries()
        }

        fun loadInquiries() {
            loadInquiriesJob?.cancel()
            loadedCursors.clear()
            val session = sessionManager.getSnapshot()
            if (!session.isTeamMemberSessionActive()) {
                mutableUiState.value =
                    mutableUiState.value.copy(
                        listState = InquiryListState.Failed(InquiryError.LoginRequired),
                    )
                return
            }
            mutableUiState.value = mutableUiState.value.copy(listState = InquiryListState.Loading)
            requestInquiryPage(
                cursor = null,
                previousInquiries = null,
                owningGeneration = session.generation,
            )
        }

        fun loadMoreInquiries() {
            val loadedState = mutableUiState.value.listState as? InquiryListState.Loaded ?: return
            val nextCursor = loadedState.nextCursor ?: return
            if (loadedState.isLoadingMore) return
            val session = sessionManager.getSnapshot()
            if (!session.isTeamMemberSessionActive()) {
                mutableUiState.value =
                    mutableUiState.value.copy(
                        listState = loadedState.afterLoadMoreFailure(InquiryError.LoginRequired),
                    )
                return
            }
            if (!loadedState.belongsToSession(session.generation)) {
                loadInquiries()
                return
            }
            mutableUiState.value =
                mutableUiState.value.copy(
                    listState = loadedState.copy(isLoadingMore = true, loadMoreError = null),
                )
            requestInquiryPage(nextCursor, loadedState.inquiries, session.generation)
        }

        private fun requestInquiryPage(
            cursor: String?,
            previousInquiries: List<InquirySummary>?,
            owningGeneration: Long,
        ) {
            loadInquiriesJob =
                viewModelScope.launch {
                    val result =
                        getInquiriesUseCase(
                            cursor = cursor,
                            expectedSessionGeneration = owningGeneration,
                        )
                    if (!isCurrentInquirySessionGeneration(owningGeneration, sessionManager.getSnapshot().generation)) {
                        return@launch
                    }
                    when (result) {
                        is ApiResult.Success -> {
                            cursor?.let(loadedCursors::add)
                            val receivedCursor = result.value.nextCursor
                            val repeatedCursor =
                                receivedCursor != null && receivedCursor in loadedCursors
                            val nextCursor = if (repeatedCursor) null else receivedCursor
                            val inquiries =
                                (previousInquiries.orEmpty() + result.value.inquiries)
                                    .distinctBy(InquirySummary::inquiryId)
                            if (cursor == null && hasPendingSubmissionGuard) {
                                verifySubmissionGuard(owningGeneration, inquiries)
                            }
                            if (!isCurrentInquirySessionGeneration(
                                    owningGeneration,
                                    sessionManager.getSnapshot().generation,
                                )
                            ) {
                                return@launch
                            }
                            val loadMoreError =
                                if (repeatedCursor) {
                                    InquiryError.Unknown
                                } else {
                                    null
                                }
                            val loadedState =
                                InquiryListState.Loaded(
                                    inquiries = inquiries,
                                    nextCursor = nextCursor,
                                    loadMoreError = loadMoreError,
                                    sessionGeneration = owningGeneration,
                                )
                            mutableUiState.value = mutableUiState.value.copy(listState = loadedState)
                        }

                        is ApiResult.Failure -> {
                            if (!isCurrentInquirySessionGeneration(
                                    owningGeneration,
                                    sessionManager.getSnapshot().generation,
                                )
                            ) {
                                return@launch
                            }
                            val error =
                                result.error.toInquiryError(
                                    sessionManager.getSnapshot().isTeamMemberSessionActive(),
                                )
                            val loadedState = mutableUiState.value.listState as? InquiryListState.Loaded
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    listState =
                                        if (previousInquiries == null || loadedState == null) {
                                            InquiryListState.Failed(error)
                                        } else {
                                            loadedState.afterLoadMoreFailure(error)
                                        },
                                )
                        }
                    }
                }
        }

        fun updateTitle(title: String) {
            mutableUiState.value =
                mutableUiState.value.copy(
                    title = title,
                    submissionError = null,
                    submissionCheckResult = null,
                )
        }

        fun updateContent(content: String) {
            mutableUiState.value =
                mutableUiState.value.copy(
                    content = content.takeCodePoints(INQUIRY_CONTENT_LIMIT),
                    submissionError = null,
                    submissionCheckResult = null,
                )
        }

        fun submitInquiry() {
            val currentState = mutableUiState.value
            if (!currentState.canSubmitInquiry()) return
            val session = sessionManager.getSnapshot()
            if (!session.isTeamMemberSessionActive()) {
                mutableUiState.value = currentState.copy(submissionError = InquiryError.LoginRequired)
                return
            }
            mutableUiState.value =
                currentState.copy(
                    isSubmitting = true,
                    submissionError = null,
                    submissionCheckResult = null,
                )
            submitInquiryJob =
                viewModelScope.launch {
                    val ownerId = session.sessionIdentifier
                    if (ownerId.isNullOrBlank()) {
                        mutableUiState.value =
                            mutableUiState.value.copy(
                                isSubmitting = false,
                                submissionError = InquiryError.LoginRequired,
                            )
                        return@launch
                    }
                    val baselineIds =
                        (currentState.listState as InquiryListState.Loaded)
                            .inquiries
                            .mapTo(mutableSetOf(), InquirySummary::inquiryId)
                    val markerResult =
                        submissionGuardUseCase.begin(
                            ownerId,
                            baselineIds,
                            currentState.title,
                            currentState.content,
                        )
                    if (!isCurrentInquirySessionGeneration(
                            session.generation,
                            sessionManager.getSnapshot().generation,
                        )
                    ) {
                        return@launch
                    }
                    when (markerResult) {
                        is ApiResult.Failure -> {
                            if (session.generation != sessionManager.getSnapshot().generation) return@launch
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    isSubmitting = false,
                                    isSubmissionGuardReady = false,
                                    requiresListCheckBeforeResubmission = true,
                                    submissionError = InquiryError.LocalStorage,
                                )
                            return@launch
                        }

                        is ApiResult.Success -> {
                            activeGuardOwnerId = ownerId
                            hasPendingSubmissionGuard = true
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    requiresListCheckBeforeResubmission = true,
                                )
                        }
                    }
                    if (!isCurrentInquirySessionGeneration(
                            session.generation,
                            sessionManager.getSnapshot().generation,
                        )
                    ) {
                        return@launch
                    }
                    when (
                        val result =
                            submitInquiryUseCase(
                                session.generation,
                                currentState.title,
                                currentState.content,
                            )
                    ) {
                        is ApiResult.Success -> {
                            handleSubmissionSuccess(session.generation, ownerId, currentState)
                        }

                        is ApiResult.Failure -> {
                            handleSubmissionFailure(session.generation, ownerId, result.error)
                        }
                    }
                }
        }

        private suspend fun handleSubmissionSuccess(
            generation: Long,
            ownerId: String,
            submittedState: InquiryUiState,
        ) {
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            val clearResult = submissionGuardUseCase.clear(ownerId)
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            when (clearResult) {
                is ApiResult.Success -> {
                    hasPendingSubmissionGuard = false
                    val currentState = mutableUiState.value
                    val draftUnchanged =
                        currentState.title == submittedState.title &&
                            currentState.content == submittedState.content
                    mutableUiState.value =
                        currentState.copy(
                            title = if (draftUnchanged) "" else currentState.title,
                            content = if (draftUnchanged) "" else currentState.content,
                            isSubmitting = false,
                            isSubmissionGuardReady = true,
                            requiresListCheckBeforeResubmission = false,
                        )
                }

                is ApiResult.Failure -> {
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            isSubmitting = false,
                            isSubmissionGuardReady = false,
                            submissionError = InquiryError.LocalStorage,
                        )
                }
            }
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            effectChannel.send(InquiryEffect.NavigateToList)
        }

        private suspend fun handleSubmissionFailure(
            generation: Long,
            ownerId: String,
            error: ApiError,
        ) {
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            val isTeamMemberSessionActive = sessionManager.getSnapshot().isTeamMemberSessionActive()
            val inquiryError = error.toInquiryError(isTeamMemberSessionActive)
            if (error.requiresInquiryListCheck()) {
                mutableUiState.value =
                    mutableUiState.value.copy(
                        isSubmitting = false,
                        requiresListCheckBeforeResubmission = true,
                        submissionError = inquiryError,
                    )
                return
            }
            val clearResult = submissionGuardUseCase.clear(ownerId)
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            when (clearResult) {
                is ApiResult.Success -> {
                    hasPendingSubmissionGuard = false
                    applySubmissionFailure(inquiryError, requiresCheck = false)
                }

                is ApiResult.Failure -> {
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            isSubmitting = false,
                            isSubmissionGuardReady = false,
                            requiresListCheckBeforeResubmission = true,
                            submissionError = InquiryError.LocalStorage,
                        )
                }
            }
        }

        private fun String.takeCodePoints(maxCodePoints: Int): String {
            if (codePointCount(0, length) <= maxCodePoints) return this
            return substring(0, offsetByCodePoints(0, maxCodePoints))
        }

        private fun applySubmissionFailure(
            error: InquiryError,
            requiresCheck: Boolean,
        ) {
            mutableUiState.value = mutableUiState.value.afterSubmissionFailure(error, requiresCheck)
        }

        fun retrySubmissionGuardAndVerification() {
            val session = sessionManager.getSnapshot()
            val ownerId = session.sessionIdentifier
            if (!session.isTeamMemberSessionActive() || ownerId.isNullOrBlank()) {
                mutableUiState.value =
                    mutableUiState.value.copy(
                        submissionError = InquiryError.LoginRequired,
                    )
                return
            }
            restoreSubmissionGuard(session.generation, ownerId) { loadInquiries() }
        }

        private fun restoreSubmissionGuard(
            generation: Long,
            ownerId: String,
            onRestored: (() -> Unit)? = null,
        ) {
            if (activeGuardOwnerId == ownerId && mutableUiState.value.isSubmissionGuardReady) {
                onRestored?.invoke()
                return
            }
            guardRestoreJob?.cancel()
            mutableUiState.value = mutableUiState.value.copy(isSubmissionGuardReady = false)
            guardRestoreJob =
                viewModelScope.launch {
                    when (val restored = submissionGuardUseCase.restore(ownerId)) {
                        is ApiResult.Success -> {
                            if (generation != sessionManager.getSnapshot().generation) return@launch
                            activeGuardOwnerId = ownerId
                            hasPendingSubmissionGuard = restored.value != null
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    isSubmissionGuardReady = true,
                                    requiresListCheckBeforeResubmission = hasPendingSubmissionGuard,
                                    submissionError =
                                        if (hasPendingSubmissionGuard) {
                                            InquiryError.ListCheckRequired
                                        } else {
                                            null
                                        },
                                )
                            onRestored?.invoke()
                            if (hasPendingSubmissionGuard &&
                                onRestored == null &&
                                mutableUiState.value.listState is InquiryListState.Loaded
                            ) {
                                loadInquiries()
                            }
                        }

                        is ApiResult.Failure -> {
                            if (generation != sessionManager.getSnapshot().generation) return@launch
                            mutableUiState.value =
                                mutableUiState.value.copy(
                                    isSubmissionGuardReady = false,
                                    requiresListCheckBeforeResubmission = true,
                                    submissionError = InquiryError.LocalStorage,
                                )
                        }
                    }
                }
        }

        private suspend fun verifySubmissionGuard(
            generation: Long,
            inquiries: List<InquirySummary>,
        ) {
            if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
            val ownerId = activeGuardOwnerId ?: return
            val draftAtCheckStart = mutableUiState.value
            when (
                val result =
                    submissionGuardUseCase.confirmAndClear(
                        ownerId = ownerId,
                        inquiries = inquiries,
                        currentTitle = draftAtCheckStart.title,
                        currentContent = draftAtCheckStart.content,
                    )
            ) {
                is ApiResult.Success -> {
                    if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
                    val confirmation = result.value ?: return
                    hasPendingSubmissionGuard = false
                    mutableUiState.value =
                        mutableUiState.value
                            .afterSubmissionGuardConfirmation(
                                submittedDraftIsUnchanged = confirmation.submittedDraftIsUnchanged,
                                titleAtCheckStart = draftAtCheckStart.title,
                                contentAtCheckStart = draftAtCheckStart.content,
                            ).copy(isSubmissionGuardReady = true)
                }

                is ApiResult.Failure -> {
                    if (!isCurrentInquirySessionGeneration(generation, sessionManager.getSnapshot().generation)) return
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            isSubmissionGuardReady = false,
                            requiresListCheckBeforeResubmission = true,
                            submissionError = InquiryError.LocalStorage,
                        )
                }
            }
        }

        private fun com.nativelap.heartguard.core.session.SessionSnapshot.isTeamMemberSessionActive(): Boolean =
            role == SessionRole.TEAM_MEMBER && !accessToken.isNullOrBlank()

        private companion object {
            const val INQUIRY_CONTENT_LIMIT = 500
        }
    }

internal fun isCurrentInquirySessionGeneration(
    requestGeneration: Long,
    currentGeneration: Long,
): Boolean = requestGeneration == currentGeneration

internal fun hasInquirySessionChanged(
    previousSession: SessionSnapshot?,
    currentSession: SessionSnapshot,
): Boolean =
    previousSession != null &&
        (
            previousSession.generation != currentSession.generation ||
                previousSession.sessionIdentifier != currentSession.sessionIdentifier
        )

internal fun hasObservedInquirySessionIdentity(
    observedSession: SessionSnapshot?,
    currentSession: SessionSnapshot,
): Boolean =
    observedSession != null &&
        observedSession.generation == currentSession.generation &&
        observedSession.sessionIdentifier == currentSession.sessionIdentifier

internal fun hasSessionScopedInquiryState(
    hasActiveJobs: Boolean,
    hasLoadedCursors: Boolean,
    hasGuardOwner: Boolean,
    hasPendingSubmission: Boolean,
    uiStateIsDefault: Boolean,
): Boolean =
    hasActiveJobs ||
        hasLoadedCursors ||
        hasGuardOwner ||
        hasPendingSubmission ||
        !uiStateIsDefault

internal fun shouldResetInquiryStateForSessionTransition(
    previousSession: SessionSnapshot?,
    currentSession: SessionSnapshot,
): Boolean {
    val currentIsActiveTeamSession =
        currentSession.role == SessionRole.TEAM_MEMBER && !currentSession.accessToken.isNullOrBlank()
    if (previousSession == null) return !currentIsActiveTeamSession

    val previousWasActiveTeamSession =
        previousSession.role == SessionRole.TEAM_MEMBER && !previousSession.accessToken.isNullOrBlank()
    return hasInquirySessionChanged(previousSession, currentSession) ||
        (previousWasActiveTeamSession && !currentIsActiveTeamSession)
}
