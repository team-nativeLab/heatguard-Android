package com.nativelap.heartguard.viewmodel.inquiry

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
import com.nativelap.heartguard.domain.inquiry.model.InquirySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class InquiryErrorStateTest {
    @Test
    fun inquiryStateResetsWhenSessionGenerationOrOwnerChangesButNotOnSameSessionRefresh() {
        val originalSession =
            SessionSnapshot(
                generation = 3L,
                accessToken = "old-token",
                sessionIdentifier = "owner-a",
            )
        val sameSessionRefresh = originalSession.copy(accessToken = "refreshed-token")
        val changedGeneration = originalSession.copy(generation = 4L)
        val changedOwner = originalSession.copy(sessionIdentifier = "owner-b")

        assertFalse(hasInquirySessionChanged(originalSession, sameSessionRefresh))
        assertTrue(hasInquirySessionChanged(originalSession, changedGeneration))
        assertTrue(hasInquirySessionChanged(originalSession, changedOwner))
    }

    @Test
    fun inquiryStateResetsWhenWithdrawalClearsTokenWithoutChangingSessionIdentity() {
        val activeSession =
            SessionSnapshot(
                generation = 3L,
                accessToken = "active-token",
                role = SessionRole.TEAM_MEMBER,
                sessionIdentifier = "owner-a",
            )
        val withdrawnSession = activeSession.copy(accessToken = null)

        assertTrue(shouldResetInquiryStateForSessionTransition(activeSession, withdrawnSession))
        assertFalse(shouldResetInquiryStateForSessionTransition(withdrawnSession, withdrawnSession))
    }

    @Test
    fun observedTokenlessSessionPreventsDuplicateStateReset() {
        val withdrawnSession =
            SessionSnapshot(
                generation = 3L,
                accessToken = null,
                role = SessionRole.TEAM_MEMBER,
                sessionIdentifier = "owner-a",
            )

        assertTrue(hasObservedInquirySessionIdentity(withdrawnSession, withdrawnSession))
        assertFalse(hasObservedInquirySessionIdentity(null, withdrawnSession))
        assertFalse(
            hasObservedInquirySessionIdentity(
                withdrawnSession,
                withdrawnSession.copy(generation = 4L),
            ),
        )
        assertFalse(
            hasObservedInquirySessionIdentity(
                withdrawnSession,
                withdrawnSession.copy(sessionIdentifier = "owner-b"),
            ),
        )
    }

    @Test
    fun sessionScopedInquiryResetOnlyRunsWhenThereIsStateToClear() {
        val hasNoStateToClear =
            hasSessionScopedInquiryState(
                hasActiveJobs = false,
                hasLoadedCursors = false,
                hasGuardOwner = false,
                hasPendingSubmission = false,
                uiStateIsDefault = true,
            )
        val hasStateToClear =
            hasSessionScopedInquiryState(
                hasActiveJobs = false,
                hasLoadedCursors = true,
                hasGuardOwner = false,
                hasPendingSubmission = false,
                uiStateIsDefault = true,
            )

        assertFalse(hasNoStateToClear)
        assertTrue(hasStateToClear)
    }

    @Test
    fun inquiryStateIsPreservedWhenActiveSessionTokenRefreshes() {
        val activeSession =
            SessionSnapshot(
                generation = 3L,
                accessToken = "old-token",
                role = SessionRole.TEAM_MEMBER,
                sessionIdentifier = "owner-a",
            )
        val refreshedSession = activeSession.copy(accessToken = "refreshed-token")

        assertFalse(shouldResetInquiryStateForSessionTransition(activeSession, refreshedSession))
    }

    @Test
    fun responseFromPreviousSessionGenerationIsStale() {
        assertFalse(isCurrentInquirySessionGeneration(requestGeneration = 3L, currentGeneration = 4L))
        assertTrue(isCurrentInquirySessionGeneration(requestGeneration = 4L, currentGeneration = 4L))
    }

    @Test
    fun loadedInquiryPagesCannotBeContinuedByAnotherSession() {
        val firstSessionPage =
            InquiryListState.Loaded(
                inquiries = emptyList(),
                nextCursor = "next",
                sessionGeneration = 3L,
            )

        assertFalse(firstSessionPage.belongsToSession(generation = 4L))
        assertTrue(firstSessionPage.belongsToSession(generation = 3L))
    }

    @Test
    fun inquiryCannotBeSubmittedUntilFirstPageLoadsSuccessfully() {
        val title = "제목"
        val content = "내용"
        val loadingState = InquiryUiState(title = title, content = content)
        val failedListState =
            loadingState.copy(
                listState = InquiryListState.Failed(InquiryError.Network),
            )
        val loadedState =
            loadingState.copy(
                listState = InquiryListState.Loaded(inquiries = emptyList()),
                isSubmissionGuardReady = true,
            )

        assertFalse(loadingState.canSubmitInquiry())
        assertFalse(failedListState.canSubmitInquiry())
        assertTrue(loadedState.canSubmitInquiry())
    }

    @Test
    fun sessionChangedWithoutTeamSessionMeansLoginRequired() {
        val inquiryError = ApiError.SessionChanged.toInquiryError(isTeamMemberSessionActive = false)

        assertEquals(InquiryError.LoginRequired, inquiryError)
    }

    @Test
    fun sessionChangedDuringAnActiveSessionIsNotReportedAsLoggedOut() {
        val inquiryError = ApiError.SessionChanged.toInquiryError(isTeamMemberSessionActive = true)

        assertEquals(InquiryError.Unknown, inquiryError)
    }

    @Test
    fun networkHttpAndResponseErrorsRemainDistinct() {
        assertEquals(InquiryError.Network, ApiError.Network.toInquiryError(isTeamMemberSessionActive = true))
        assertEquals(InquiryError.RequestRejected, ApiError.Http(400).toInquiryError(isTeamMemberSessionActive = true))
        assertEquals(InquiryError.Server, ApiError.Http(503).toInquiryError(isTeamMemberSessionActive = true))
        assertEquals(
            InquiryError.UnreadableResponse,
            ApiError.Serialization.toInquiryError(isTeamMemberSessionActive = true),
        )
    }

    @Test
    fun submissionRemainsDisabledUntilPersistentGuardRestores() {
        val state =
            InquiryUiState(
                title = "제목",
                content = "내용",
                listState = InquiryListState.Loaded(inquiries = emptyList()),
            )

        assertFalse(state.canSubmitInquiry())
        assertTrue(state.copy(isSubmissionGuardReady = true).canSubmitInquiry())
    }

    @Test
    fun restoredPendingSubmissionRemainsLockedUntilMatchingInquiryIsConfirmed() {
        val restoredState =
            InquiryUiState(
                title = "제목",
                content = "내용",
                isSubmissionGuardReady = true,
                requiresListCheckBeforeResubmission = true,
                listState = InquiryListState.Loaded(inquiries = emptyList()),
            )

        assertFalse(restoredState.canSubmitInquiry())
        assertFalse(
            restoredState
                .afterInquiryListVerification(
                    found = false,
                    attemptedTitle = "제목",
                    attemptedContent = "내용",
                ).canSubmitInquiry(),
        )
        val confirmedState =
            restoredState.afterInquiryListVerification(
                found = true,
                attemptedTitle = "제목",
                attemptedContent = "내용",
            )

        assertFalse(confirmedState.requiresListCheckBeforeResubmission)
        assertTrue(confirmedState.copy(title = "새 제목", content = "새 내용").canSubmitInquiry())
    }

    @Test
    fun guardStorageFailureKeepsInquirySubmissionDisabled() {
        val storageFailureState =
            InquiryUiState(
                title = "제목",
                content = "내용",
                isSubmissionGuardReady = false,
                requiresListCheckBeforeResubmission = true,
                submissionError = InquiryError.LocalStorage,
                listState = InquiryListState.Loaded(inquiries = emptyList()),
            )

        assertFalse(storageFailureState.canSubmitInquiry())
        assertEquals(InquiryError.LocalStorage, storageFailureState.submissionError)
    }

    @Test
    fun ambiguousSubmissionFailureBlocksResubmitUntilSuccessfulListCheck() {
        val stateAfterFailure =
            InquiryUiState(
                title = "제목",
                content = "내용",
                isSubmissionGuardReady = true,
                listState = InquiryListState.Loaded(inquiries = emptyList()),
            ).afterSubmissionFailure(
                error = InquiryError.Network,
                requiresInquiryListCheck = true,
            )

        assertFalse(stateAfterFailure.canSubmitInquiry())

        val stateAfterNoMatch =
            stateAfterFailure.afterInquiryListVerification(
                found = false,
                attemptedTitle = "제목",
                attemptedContent = "내용",
            )

        assertFalse(stateAfterNoMatch.canSubmitInquiry())
        assertTrue(stateAfterNoMatch.requiresListCheckBeforeResubmission)
    }

    @Test
    fun matchingNewInquiryPreservesDraftChangedAfterSubmission() {
        val stateAfterFailure =
            InquiryUiState(
                title = "수정한 제목",
                content = "수정한 내용",
                requiresListCheckBeforeResubmission = true,
            )

        val verifiedState =
            stateAfterFailure.afterInquiryListVerification(
                found = true,
                attemptedTitle = "등록 시 제목",
                attemptedContent = "등록 시 내용",
            )

        assertEquals("수정한 제목", verifiedState.title)
        assertEquals("수정한 내용", verifiedState.content)
        assertFalse(verifiedState.requiresListCheckBeforeResubmission)
        assertEquals(InquirySubmissionCheckResult.Found, verifiedState.submissionCheckResult)
    }

    @Test
    fun matchingNewInquiryClearsUnchangedSubmittedDraft() {
        val verifiedState =
            InquiryUiState(
                title = "제목",
                content = "내용",
                requiresListCheckBeforeResubmission = true,
            ).afterInquiryListVerification(
                found = true,
                attemptedTitle = "제목",
                attemptedContent = "내용",
            )

        assertEquals("", verifiedState.title)
        assertEquals("", verifiedState.content)
        assertFalse(verifiedState.requiresListCheckBeforeResubmission)
    }

    @Test
    fun confirmationDoesNotClearDraftEditedWhileListCheckWasRunning() {
        val stateAfterConcurrentEdit =
            InquiryUiState(
                title = "확인 시작 시 제목",
                content = "확인 시작 시 내용",
                requiresListCheckBeforeResubmission = true,
            ).copy(
                title = "새로 편집한 제목",
                content = "새로 편집한 내용",
            )

        val confirmedState =
            stateAfterConcurrentEdit.afterSubmissionGuardConfirmation(
                submittedDraftIsUnchanged = true,
                titleAtCheckStart = "확인 시작 시 제목",
                contentAtCheckStart = "확인 시작 시 내용",
            )

        assertEquals("새로 편집한 제목", confirmedState.title)
        assertEquals("새로 편집한 내용", confirmedState.content)
        assertFalse(confirmedState.requiresListCheckBeforeResubmission)
        assertEquals(InquirySubmissionCheckResult.Found, confirmedState.submissionCheckResult)
    }

    @Test
    fun loadMoreFailureKeepsPreviouslyLoadedInquiriesAndStoresError() {
        val existingInquiry = inquirySummary("existing-id", "기존 문의", "기존 내용")
        val loadedState = InquiryListState.Loaded(inquiries = listOf(existingInquiry), nextCursor = "next")

        val failedState = loadedState.afterLoadMoreFailure(InquiryError.LoginRequired) as InquiryListState.Loaded

        assertEquals(listOf(existingInquiry), failedState.inquiries)
        assertEquals(InquiryError.LoginRequired, failedState.loadMoreError)
        assertFalse(failedState.isLoadingMore)
    }

    private fun inquirySummary(
        inquiryId: String,
        title: String,
        content: String,
    ) = InquirySummary(
        inquiryId = inquiryId,
        title = title,
        status = InquiryStatus.OPEN,
        replyCount = 0,
        createdAt = OffsetDateTime.parse("2026-11-23T09:00:00Z"),
        content = content,
    )
}
