package com.nativelap.heartguard.viewmodel.record

import com.nativelap.heartguard.domain.record.usecase.GetPendingRecordSubmissionsUseCase

import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult

import com.nativelap.heartguard.domain.profile.model.ProfileUpdateResult

import com.nativelap.heartguard.domain.profile.model.WorkerProfile

import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository

import com.nativelap.heartguard.domain.profile.usecase.GetWorkerProfileUseCase

import com.nativelap.heartguard.domain.record.model.PendingRecordSubmission

import com.nativelap.heartguard.domain.record.repository.RecordSubmissionRepository

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import com.nativelap.heartguard.domain.record.usecase.SubmitFieldRecordUseCase
import com.nativelap.heartguard.domain.record.usecase.UploadFieldPhotosUseCase
import com.nativelap.heartguard.view.component.RecordType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDraftSavedStateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun selectedKoreaDateAndTimesRestoreAndReselectingStartClearsEnd() {
        val sessionManager = authenticatedSession()
        instrumentation.runOnMainSync {
            val savedState = SavedStateHandle()
            val viewModel = createViewModel(savedState, sessionManager)
            viewModel.selectRecordType(RecordType.REST)
            viewModel.updateRestStartTime(LocalTime.of(23, 50, 59, 123))
            viewModel.updateRestEndTime(LocalTime.of(0, 10, 59, 123))
            assertEquals(LocalDate.of(2026, 10, 1), viewModel.uiState.value.restDate)
            assertEquals(LocalTime.of(23, 50), viewModel.uiState.value.restStartTime)
            assertEquals(LocalTime.of(0, 10), viewModel.uiState.value.restEndTime)
            val snapshot = savedState.keys().associateWith { key -> savedState.get<Any?>(key) }
            val restoredState = SavedStateHandle(snapshot)
            val restoredViewModel = createViewModel(restoredState, sessionManager)
            try {
                assertEquals(viewModel.uiState.value, restoredViewModel.uiState.value)
                assertTrue(restoredViewModel.uiState.value.hasValidRestTimeRange)
                restoredViewModel.updateRestStartTime(LocalTime.of(13, 0))
                assertNull(restoredViewModel.uiState.value.restEndTime)
                assertNull(restoredState.get<String>("record_draft.rest_end_time"))
                restoredViewModel.reset()
                assertEquals(RecordDraftUiState(), restoredViewModel.uiState.value)
                assertFalse(restoredState.keys().contains("record_draft.rest_date"))
            } finally {
                viewModel.viewModelScope.cancel()
                restoredViewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun malformedSavedTimesAreIgnoredAndSessionEndClearsRestDraft() {
        val sessionManager = authenticatedSession()
        lateinit var viewModel: RecordDraftViewModel
        val savedState = SavedStateHandle(
            mapOf(
                "record_draft.rest_date" to "invalid-date",
                "record_draft.rest_start_time" to "25:70",
                "record_draft.rest_end_time" to "broken",
            ),
        )
        instrumentation.runOnMainSync {
            viewModel = createViewModel(savedState, sessionManager)
            assertFalse(viewModel.uiState.value.hasValidRestTimeRange)
            assertNull(viewModel.uiState.value.restDate)
            assertNull(viewModel.uiState.value.restStartTime)
            assertNull(viewModel.uiState.value.restEndTime)
            viewModel.updateRestStartTime(LocalTime.of(13, 0))
            viewModel.updateRestEndTime(LocalTime.of(13, 30))
        }
        runBlocking { sessionManager.expireSession() }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            try {
                assertEquals(RecordDraftUiState(), viewModel.uiState.value)
                assertFalse(savedState.keys().contains("record_draft.rest_date"))
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun quickReloginWithAnotherAccountClearsDraft() {
        val sessionManager = authenticatedSession()
        val savedState = SavedStateHandle()
        lateinit var viewModel: RecordDraftViewModel
        instrumentation.runOnMainSync {
            viewModel = createViewModel(savedState, sessionManager)
            viewModel.selectRecordType(RecordType.REST)
            viewModel.updateRestStartTime(LocalTime.of(13, 0))
            viewModel.updateRestEndTime(LocalTime.of(13, 30))
        }
        instrumentation.waitForIdleSync()
        runBlocking {
            sessionManager.expireSession()
            sessionManager.onLoginSucceeded(SessionToken("second-account-token"))
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            try {
                assertEquals(RecordDraftUiState(), viewModel.uiState.value)
                assertFalse(savedState.keys().contains("record_draft.rest_date"))
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun restoringStoredSessionAtAppStartKeepsDraft() {
        val sessionManager = authenticatedSession()
        val savedState = previousProcessRestDraft()
        lateinit var viewModel: RecordDraftViewModel
        instrumentation.runOnMainSync {
            viewModel = createViewModel(savedState, sessionManager)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            try {
                assertEquals(LocalTime.of(13, 0), viewModel.uiState.value.restStartTime)
                assertEquals(LocalTime.of(13, 30), viewModel.uiState.value.restEndTime)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun freshLoginDiscardsDraftLeftByPreviousProcess() {
        val sessionManager = SessionManager(EmptyTokenStorage(), Dispatchers.Unconfined)
        runBlocking {
            sessionManager.initialize()
            sessionManager.onLoginSucceeded(SessionToken("another-worker-token"))
        }
        val savedState = previousProcessRestDraft()
        lateinit var viewModel: RecordDraftViewModel
        instrumentation.runOnMainSync {
            viewModel = createViewModel(savedState, sessionManager)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            try {
                assertEquals(RecordDraftUiState(), viewModel.uiState.value)
                assertFalse(savedState.keys().contains("record_draft.rest_start_time"))
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun accountSwitchDuringPhotoUploadDoesNotSubmitRecord() {
        val sessionManager = authenticatedSession()
        val repository = SessionSwitchingRecordRepository(sessionManager)
        lateinit var viewModel: RecordDraftViewModel
        instrumentation.runOnMainSync {
            viewModel = createViewModel(SavedStateHandle(), sessionManager, repository)
            viewModel.selectRecordType(RecordType.WORK)
            viewModel.addPhoto(RecordType.WORK, Uri.parse("content://heartguard.test/photo/1"))
            viewModel.submit()
        }
        instrumentation.waitForIdleSync()
        runBlocking { repository.uploadFinished.await() }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            try {
                assertEquals(0, repository.submitCount)
                assertFalse(viewModel.submissionState.value is RecordSubmissionState.Failure)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun restoredUnknownSubmissionDoesNotUploadOrPost() {
        val session = authenticatedSession()
        val repository = CountingRecordRepository()
        val savedState = previousProcessRestDraft()
        savedState["record_draft.submission_id"] = "unknown-intent"
        savedState["record_draft.submission_measured_at"] = "2026-10-01T01:00:00+09:00"
        instrumentation.runOnMainSync {
            val viewModel = createViewModel(
                savedState,
                session,
                repository,
                listOf(PendingRecordSubmission("unknown-intent", FieldRecordType.REST, OffsetDateTime.parse("2026-10-01T01:00:00+09:00"))),
            )
            try {
                viewModel.addPhoto(RecordType.REST, Uri.parse("content://photos/restored"))
                viewModel.submit()
                assertTrue(viewModel.submissionState.value is RecordSubmissionState.Unknown)
                assertEquals(0, repository.uploadCount)
                assertEquals(0, repository.postCount)
                viewModel.submit()
                assertEquals(0, repository.uploadCount)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun restoredSubmissionWithUnavailableJournalRemainsUnknown() {
        val session = authenticatedSession()
        val repository = CountingRecordRepository()
        val savedState = previousProcessRestDraft()
        savedState["record_draft.submission_id"] = "previous-intent"
        savedState["record_draft.submission_measured_at"] = "2026-10-01T01:00:00+09:00"
        instrumentation.runOnMainSync {
            val viewModel = createViewModel(
                savedState = savedState,
                sessionManager = session,
                repository = repository,
                pendingReadError = ApiError.LocalStorage,
            )
            try {
                viewModel.addPhoto(RecordType.REST, Uri.parse("content://photos/restored"))
                viewModel.submit()
                assertTrue(viewModel.submissionState.value is RecordSubmissionState.Unknown)
                assertEquals(0, repository.uploadCount)
                assertEquals(0, repository.postCount)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    private class CountingRecordRepository : RecordRepository {
        var uploadCount = 0
        var postCount = 0
        override suspend fun uploadFieldPhotos(
            photoUris: List<Uri>,
            alreadyUploadedPhotoKeys: Map<Uri, String>,
            onPhotoUploaded: (Uri, String) -> Unit,
        ): ApiResult<List<String>> {
            uploadCount += 1
            return ApiResult.Success(listOf("uploaded"))
        }
        override suspend fun submitFieldRecord(
            expectedSessionGeneration: Long,
            type: FieldRecordType,
            photoKeys: List<String>,
            measuredAt: OffsetDateTime,
            temperature: Double?,
            humidity: Double?,
            memo: String?,
            restStartedAt: OffsetDateTime?,
            restEndedAt: OffsetDateTime?,
        ): ApiResult<FieldRecord> {
            postCount += 1
            return ApiResult.Success(FieldRecord("record", null, null, emptyList(), null))
        }
    }

    private fun previousProcessRestDraft(): SavedStateHandle = SavedStateHandle(
        mapOf(
            "record_draft.selected_record_type" to RecordType.REST.name,
            "record_draft.rest_date" to "2026-10-01",
            "record_draft.rest_start_time" to "13:00",
            "record_draft.rest_end_time" to "13:30",
        ),
    )

    private fun authenticatedSession(): SessionManager {
        val sessionManager = SessionManager(TestTokenStorage(), Dispatchers.Unconfined)
        runBlocking { sessionManager.initialize() }
        return sessionManager
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
        sessionManager: SessionManager,
        repository: RecordRepository = NoNetworkRecordRepository(),
        pendingEntries: List<PendingRecordSubmission> = emptyList(),
        pendingReadError: ApiError? = null,
    ): RecordDraftViewModel {
        val journal = object : RecordSubmissionRepository {
            private val entries = pendingEntries.associateBy { entry -> entry.submissionId }.toMutableMap()
            override suspend fun getPendingSubmissions(userId: String): ApiResult<List<PendingRecordSubmission>> {
                return if (pendingReadError != null) {
                    ApiResult.Failure(pendingReadError)
                } else {
                    ApiResult.Success(entries.values.toList())
                }
            }
            override suspend fun beginSubmission(userId: String, submission: PendingRecordSubmission): ApiResult<Boolean> {
                if (submission.submissionId in entries) {
                    return ApiResult.Success(false)
                }
                entries[submission.submissionId] = submission
                return ApiResult.Success(true)
            }
            override suspend fun completeSubmission(userId: String, submissionId: String): ApiResult<Unit> {
                entries.remove(submissionId)
                return ApiResult.Success(Unit)
            }
        }
        val profile = GetWorkerProfileUseCase(
            object : WorkerProfileRepository {
                override suspend fun getWorkerProfile() = ApiResult.Success(
                    WorkerProfile("worker-test", null, null),
                )
                override suspend fun updateWorkerName(name: String, version: Long?) =
                    ProfileUpdateResult.Failure
                override suspend fun changePassword(currentPassword: String, newPassword: String) =
                    PasswordChangeResult.Failure
            },
        )
        return RecordDraftViewModel(
            context = instrumentation.targetContext.applicationContext,
            ioDispatcher = Dispatchers.Unconfined,
            clock = Clock.fixed(Instant.parse("2026-09-30T16:00:00Z"), ZoneOffset.UTC),
            savedStateHandle = savedState,
            sessionManager = sessionManager,
            uploadFieldPhotosUseCase = UploadFieldPhotosUseCase(repository),
            submitFieldRecordUseCase = SubmitFieldRecordUseCase(
                recordRepository = repository,
                submissionRepository = journal,
                getWorkerProfileUseCase = profile,
                sessionManager = sessionManager,
            ),
            retryRecordSubmissionCleanupUseCase = com.nativelap.heartguard.domain.record.usecase.RetryRecordSubmissionCleanupUseCase(
                submissionRepository = journal,
                getWorkerProfileUseCase = profile,
                sessionManager = sessionManager,
            ),
            getPendingRecordSubmissionsUseCase = GetPendingRecordSubmissionsUseCase(
                submissionRepository = journal,
                getWorkerProfileUseCase = profile,
                sessionManager = sessionManager,
            ),
        )
    }

    private class EmptyTokenStorage : TokenStorage {
        private var accessToken: String? = null
        override fun readAccessToken(): String? = accessToken
        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }
        override fun clear() {
            accessToken = null
        }
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String = "test-token"
        override fun saveAccessToken(accessToken: String) = Unit
        override fun clear() = Unit
    }

    private class SessionSwitchingRecordRepository(
        private val sessionManager: SessionManager,
    ) : RecordRepository {
        val uploadFinished = CompletableDeferred<Unit>()
        var submitCount = 0

        override suspend fun uploadFieldPhotos(
            photoUris: List<Uri>,
            alreadyUploadedPhotoKeys: Map<Uri, String>,
            onPhotoUploaded: (Uri, String) -> Unit,
        ): ApiResult<List<String>> {
            sessionManager.expireSession()
            sessionManager.onLoginSucceeded(SessionToken("second-account-token"))
            uploadFinished.complete(Unit)
            return ApiResult.Success(listOf("uploads/first-account-photo.jpg"))
        }

        override suspend fun submitFieldRecord(
        expectedSessionGeneration: Long,
            type: FieldRecordType,
            photoKeys: List<String>,
            measuredAt: OffsetDateTime,
            temperature: Double?,
            humidity: Double?,
            memo: String?,
            restStartedAt: OffsetDateTime?,
            restEndedAt: OffsetDateTime?,
        ): ApiResult<FieldRecord> {
            submitCount += 1
            return ApiResult.Failure(ApiError.Network)
        }
    }

    private class NoNetworkRecordRepository : RecordRepository {
        override suspend fun uploadFieldPhotos(
            photoUris: List<Uri>,
            alreadyUploadedPhotoKeys: Map<Uri, String>,
            onPhotoUploaded: (Uri, String) -> Unit,
        ): ApiResult<List<String>> = ApiResult.Failure(ApiError.Network)

        override suspend fun submitFieldRecord(
        expectedSessionGeneration: Long,
            type: FieldRecordType,
            photoKeys: List<String>,
            measuredAt: OffsetDateTime,
            temperature: Double?,
            humidity: Double?,
            memo: String?,
            restStartedAt: OffsetDateTime?,
            restEndedAt: OffsetDateTime?,
        ): ApiResult<FieldRecord> = ApiResult.Failure(ApiError.Network)
    }
}
