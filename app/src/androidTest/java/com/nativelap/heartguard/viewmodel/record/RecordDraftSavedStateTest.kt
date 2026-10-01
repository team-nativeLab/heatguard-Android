package com.nativelap.heartguard.viewmodel.record

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
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

    private fun authenticatedSession(): SessionManager {
        val sessionManager = SessionManager(TestTokenStorage(), Dispatchers.Unconfined)
        runBlocking { sessionManager.initialize() }
        return sessionManager
    }

    private fun createViewModel(
        savedState: SavedStateHandle,
        sessionManager: SessionManager,
    ): RecordDraftViewModel {
        val repository = NoNetworkRecordRepository()
        return RecordDraftViewModel(
            context = instrumentation.targetContext.applicationContext,
            ioDispatcher = Dispatchers.Unconfined,
            clock = Clock.fixed(Instant.parse("2026-09-30T16:00:00Z"), ZoneOffset.UTC),
            savedStateHandle = savedState,
            sessionManager = sessionManager,
            uploadFieldPhotosUseCase = UploadFieldPhotosUseCase(repository),
            submitFieldRecordUseCase = SubmitFieldRecordUseCase(repository),
        )
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String = "test-token"
        override fun saveAccessToken(accessToken: String) = Unit
        override fun clear() = Unit
    }

    private class NoNetworkRecordRepository : RecordRepository {
        override suspend fun uploadFieldPhotos(
            photoUris: List<Uri>,
            alreadyUploadedPhotoKeys: Map<Uri, String>,
            onPhotoUploaded: (Uri, String) -> Unit,
        ): ApiResult<List<String>> = ApiResult.Failure(ApiError.Network)

        override suspend fun submitFieldRecord(
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
