package com.nativelap.heartguard.viewmodel.record

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.util.releasePhoto
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.domain.record.usecase.SubmitFieldRecordUseCase
import com.nativelap.heartguard.domain.record.usecase.UploadFieldPhotosUseCase
import com.nativelap.heartguard.view.component.RecordType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 기록유형선택 → 온도기록/사진촬영 → 저장 전 확인까지, 여러 NavKey가 공유해야 하는 임시 입력값 저장소다.
 * [com.nativelap.heartguard.navigation.HeartGuardNavHost]의 메인 흐름 상위에서 인자 없는 hiltViewModel()로
 * 이 인스턴스를 하나만 만들어 각 Route에 명시적으로 전달한다(android-navigation SKILL '화면 간 ViewModel 공유' 참고 —
 * 다만 그 스킬이 예시로 든 수동 ViewModelStoreOwner는 이 프로젝트의 hilt-navigation-compose 버전에서
 * Hilt 팩토리를 찾지 못해 대신 기본 ViewModelStoreOwner를 그대로 쓴다). */
@HiltViewModel
class RecordDraftViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val uploadFieldPhotosUseCase: UploadFieldPhotosUseCase,
    private val submitFieldRecordUseCase: SubmitFieldRecordUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(savedStateHandle.restoreRecordDraft())
    val uiState: StateFlow<RecordDraftUiState> = mutableUiState.asStateFlow()

    private val mutableSubmissionState = MutableStateFlow<RecordSubmissionState>(RecordSubmissionState.Idle)
    val submissionState: StateFlow<RecordSubmissionState> = mutableSubmissionState.asStateFlow()

    private val submissionEffectChannel = Channel<RecordSubmissionEffect>(Channel.BUFFERED)
    val submissionEffects: Flow<RecordSubmissionEffect> = submissionEffectChannel.receiveAsFlow()

    private var submitJob: Job? = null

    // 부분 업로드 성공을 기록해 재시도 시 성공한 사진의 URL 발급과 업로드를 건너뛴다(메모리에만 둔다).
    private val uploadedPhotoKeysByUri = mutableMapOf<Uri, String>()

    init {
        viewModelScope.launch {
            sessionManager.sessionState.collect { currentSessionState ->
                if (currentSessionState == SessionState.Unauthenticated) {
                    reset()
                }
            }
        }
    }

    /** 기록유형선택 화면에서 선택을 확정할 때 호출한다. [submit]이 어떤 종류의
     * 기록(THERMOMETER/WORK/REST)을 서버에 보낼지 이 값으로 판단한다. */
    fun selectRecordType(recordType: RecordType) {
        updateDraft { it.copy(selectedRecordType = recordType) }
    }

    /** 홈의 "현장 사진"처럼 기록유형 선택 없이 특정 기록을 새로 시작할 때 호출한다.
     * 이전 시도의 입력값·임시 사진을 정리한 뒤 [recordType]으로 기록 종류를 정한다. */
    fun startRecord(recordType: RecordType) {
        reset()
        selectRecordType(recordType)
    }

    fun updateTemperatureText(temperatureText: String) {
        updateDraft { it.copy(temperatureText = temperatureText) }
    }

    fun updateHumidityText(humidityText: String) {
        updateDraft { it.copy(humidityText = humidityText) }
    }

    fun updateManualInputEnabled(isManualInputEnabled: Boolean) {
        updateDraft { it.copy(isManualInputEnabled = isManualInputEnabled) }
    }

    /** 카메라 촬영이나 앨범 선택 결과를 해당 기록의 사진 목록에 추가한다. */
    fun addPhoto(recordType: RecordType, photoUri: Uri): Boolean {
        var wasAdded = false
        mutableUiState.update { state ->
            val currentPhotoUris = state.photoUrisFor(recordType)
            if (currentPhotoUris.size >= MAX_RECORD_PHOTO_COUNT || photoUri in currentPhotoUris) {
                state
            } else {
                wasAdded = true
                state.withPhotoUris(recordType, currentPhotoUris + photoUri)
            }
        }
        return wasAdded
    }

    /** 사진 목록에서 URI를 제거하고 연결된 캐시 파일 또는 앨범 권한을 백그라운드에서 정리한다. */
    fun removePhoto(recordType: RecordType, uri: Uri) {
        uploadedPhotoKeysByUri.remove(uri)
        mutableUiState.update { state ->
            state.withPhotoUris(recordType, state.photoUrisFor(recordType) - uri)
        }
        viewModelScope.launch(ioDispatcher) {
            releasePhoto(context, uri)
        }
    }

    /** 다시 촬영을 시작할 때 현재 사진 목록과 로컬 자원을 함께 정리한다. */
    fun clearPhotos(recordType: RecordType) {
        val photoUrisToRelease = mutableUiState.value.photoUrisFor(recordType)
        photoUrisToRelease.forEach(uploadedPhotoKeysByUri::remove)
        mutableUiState.update { state ->
            state.withPhotoUris(recordType, emptyList())
        }
        viewModelScope.launch(ioDispatcher) {
            photoUrisToRelease.forEach { photoUri ->
                releasePhoto(context, photoUri)
            }
        }
    }

    fun updateWorkMemo(memo: String) {
        updateDraft { it.copy(workMemo = memo) }
    }

    fun updateRestMemo(memo: String) {
        updateDraft { it.copy(restMemo = memo) }
    }

    /** 저장 실패 화면에서 보관을 선택한 초안을 현재 세션의 기록 내역에 표시한다. */
    fun markDraftTemporarilySaved() {
        if (mutableUiState.value.selectedRecordType == null) {
            return
        }

        updateDraft { state ->
            state.copy(
                isTemporarilySaved = true,
                temporarilySavedAt = OffsetDateTime.now(clock),
            )
        }
    }

    fun resumeTemporarilySavedDraft() {
        updateDraft { state ->
            state.copy(
                isTemporarilySaved = false,
                temporarilySavedAt = null,
            )
        }
    }

    /** 온도계 기록·현장 사진·작업 사진·휴식 사진 화면의 저장 버튼을 누르면 호출한다. 선택된 기록 유형의 사진을 먼저
     * 업로드하고, 그 objectKey로 현장 기록을 저장한다. 결과는 [submissionState](SaveSuccess·SaveFailure 화면이 읽음)와
     * [submissionEffects](저장 화면의 이동)로 알린다.
     * 저장은 화면이 아니라 viewModelScope에서 돌아, 화면을 벗어나거나 회전해도 요청이 끊기지 않는다.
     * 진행 중이면 연타를 무시하고(같은 프레임의 두 번 입력도 [submitJob]으로 막는다), 요청이 취소되면 저장 전 상태로 되돌린다. */
    fun submit() {
        if (submitJob?.isActive == true) {
            return
        }

        val state = mutableUiState.value
        val recordType = state.selectedRecordType ?: return
        // 버튼 활성 조건과 같은 규칙으로 한 번 더 막아, 필수 값 없이 서버에 요청하지 않게 한다.
        if (!state.canSubmit(recordType)) {
            return
        }

        mutableSubmissionState.value = RecordSubmissionState.Submitting
        submitJob = viewModelScope.launch {
            try {
                val submitResult = uploadAndSubmit(
                    state = state,
                    recordType = recordType,
                )
                mutableSubmissionState.value = when (submitResult) {
                    is ApiResult.Success -> {
                        clearSavedDraft()
                        RecordSubmissionState.Success(submitResult.value)
                    }

                    is ApiResult.Failure -> RecordSubmissionState.Failure(submitResult.error)
                }
                submissionEffectChannel.send(
                    if (submitResult is ApiResult.Success) {
                        RecordSubmissionEffect.Succeeded
                    } else {
                        RecordSubmissionEffect.Failed
                    },
                )
            } finally {
                if (mutableSubmissionState.value == RecordSubmissionState.Submitting) {
                    mutableSubmissionState.value = RecordSubmissionState.Idle
                }
            }
        }
    }

    private suspend fun uploadAndSubmit(
        state: RecordDraftUiState,
        recordType: RecordType,
    ): ApiResult<FieldRecord> {
        val measuredAt = OffsetDateTime.now(clock)
        val photoUris = state.photoUrisFor(recordType)
        val uploadResult = uploadFieldPhotosUseCase(
            photoUris = photoUris,
            alreadyUploadedPhotoKeys = uploadedPhotoKeysByUri.toMap(),
            onPhotoUploaded = { photoUri, objectKey ->
                if (photoUri in mutableUiState.value.photoUrisFor(recordType)) {
                    uploadedPhotoKeysByUri[photoUri] = objectKey
                }
            },
        )
        val photoKeys = when (uploadResult) {
            is ApiResult.Success -> uploadResult.value
            is ApiResult.Failure -> return uploadResult
        }

        val isManualTemperature = recordType == RecordType.TEMPERATURE && state.isManualInputEnabled
        val submitResult = submitFieldRecordUseCase(
            type = recordType.toFieldRecordType(),
            photoKeys = photoKeys,
            measuredAt = measuredAt,
            temperature = if (isManualTemperature) {
                state.temperatureText.toDoubleOrNull()
            } else {
                null
            },
            humidity = if (isManualTemperature) {
                state.humidityText.toDoubleOrNull()
            } else {
                null
            },
            memo = state.memoFor(recordType),
        )
        // 업로드 키를 서버가 찾지 못하거나(만료·정리) 이미 쓴 키라면, 재시도 때 같은 키를 다시 보내지 않도록 비워 새로 올린다.
        if (submitResult is ApiResult.Failure && submitResult.error.isStaleUploadKey()) {
            photoUris.forEach(uploadedPhotoKeysByUri::remove)
        }
        return submitResult
    }

    private fun ApiError.isStaleUploadKey(): Boolean {
        val httpError = this as? ApiError.Http ?: return false
        return httpError.errorCode == UPLOAD_NOT_FOUND_CODE || httpError.errorCode == UPLOAD_ALREADY_USED_CODE
    }

    // 모든 기록 유형은 사진 1~2장이 필요하며, 온도계 수기 입력 시에도 사진을 함께 첨부한다.
    private fun RecordDraftUiState.canSubmit(recordType: RecordType): Boolean = when (recordType) {
        RecordType.TEMPERATURE -> canSubmitTemperatureRecord && fieldPhotoUris.isNotEmpty()
        RecordType.WORK -> workPhotoUris.isNotEmpty()
        RecordType.REST -> restPhotoUris.isNotEmpty()
    }

    private fun RecordDraftUiState.photoUrisFor(recordType: RecordType): List<Uri> = when (recordType) {
        RecordType.TEMPERATURE -> fieldPhotoUris
        RecordType.WORK -> workPhotoUris
        RecordType.REST -> restPhotoUris
    }

    private fun RecordDraftUiState.withPhotoUris(
        recordType: RecordType,
        photoUris: List<Uri>,
    ): RecordDraftUiState = when (recordType) {
        RecordType.TEMPERATURE -> copy(fieldPhotoUris = photoUris)
        RecordType.WORK -> copy(workPhotoUris = photoUris)
        RecordType.REST -> copy(restPhotoUris = photoUris)
    }

    private fun RecordDraftUiState.memoFor(recordType: RecordType): String? = when (recordType) {
        RecordType.TEMPERATURE -> null
        RecordType.WORK -> workMemo.ifBlank { null }
        RecordType.REST -> restMemo.ifBlank { null }
    }

    private fun RecordType.toFieldRecordType(): FieldRecordType = when (this) {
        RecordType.TEMPERATURE -> FieldRecordType.THERMOMETER
        RecordType.WORK -> FieldRecordType.WORK
        RecordType.REST -> FieldRecordType.REST
    }

    /** 기록 유형을 새로 선택하기 시작할 때(RecordTypeSelection 진입) 또는 이 흐름을 완전히 벗어날 때
     * (로그아웃·세션 만료로 HeartGuardMainNavDisplay가 컴포지션에서 사라질 때) 호출한다. 이전 시도의
     * 입력값과, 아직 서버에 올리지 않아 로컬에만 남아 있던 임시 사진 파일을 모두 정리한다.
     * UI 상태는 즉시 초기화하고, 파일 I/O는 메인 스레드를 막지 않도록 백그라운드에서 한다. */
    fun reset() {
        submitJob?.cancel()
        submitJob = null
        val photoUrisToRelease = currentPhotoUris()
        mutableUiState.value = RecordDraftUiState()
        uploadedPhotoKeysByUri.clear()
        clearSavedDraft()
        mutableSubmissionState.value = RecordSubmissionState.Idle
        viewModelScope.launch(ioDispatcher) {
            photoUrisToRelease.forEach { uri -> releasePhoto(context, uri) }
        }
    }

    override fun onCleared() {
        // viewModelScope는 이 시점에 이미 취소되므로 여기서는 동기적으로 정리한다.
        currentPhotoUris().forEach { uri -> releasePhoto(context, uri) }
    }

    private fun currentPhotoUris(): List<Uri> {
        val state = mutableUiState.value
        return (state.fieldPhotoUris + state.workPhotoUris + state.restPhotoUris).distinct()
    }

    private fun updateDraft(transform: (RecordDraftUiState) -> RecordDraftUiState) {
        mutableUiState.update(transform)
        saveDraft(mutableUiState.value)
    }

    private fun saveDraft(state: RecordDraftUiState) {
        savedStateHandle[KEY_SELECTED_RECORD_TYPE] = state.selectedRecordType?.name
        savedStateHandle[KEY_TEMPERATURE_TEXT] = state.temperatureText
        savedStateHandle[KEY_HUMIDITY_TEXT] = state.humidityText
        savedStateHandle[KEY_MANUAL_INPUT_ENABLED] = state.isManualInputEnabled
        savedStateHandle[KEY_WORK_MEMO] = state.workMemo
        savedStateHandle[KEY_REST_MEMO] = state.restMemo
        savedStateHandle[KEY_TEMPORARILY_SAVED] = state.isTemporarilySaved
        savedStateHandle[KEY_TEMPORARILY_SAVED_AT] = state.temporarilySavedAt?.toString()
    }

    private fun clearSavedDraft() {
        savedStateHandle.remove<String>(KEY_SELECTED_RECORD_TYPE)
        savedStateHandle.remove<String>(KEY_TEMPERATURE_TEXT)
        savedStateHandle.remove<String>(KEY_HUMIDITY_TEXT)
        savedStateHandle.remove<Boolean>(KEY_MANUAL_INPUT_ENABLED)
        savedStateHandle.remove<String>(KEY_WORK_MEMO)
        savedStateHandle.remove<String>(KEY_REST_MEMO)
        savedStateHandle.remove<Boolean>(KEY_TEMPORARILY_SAVED)
        savedStateHandle.remove<String>(KEY_TEMPORARILY_SAVED_AT)
    }

    private fun SavedStateHandle.restoreRecordDraft(): RecordDraftUiState {
        val selectedRecordType = get<String>(KEY_SELECTED_RECORD_TYPE)
            ?.let { name -> runCatching { RecordType.valueOf(name) }.getOrNull() }
        return RecordDraftUiState(
            selectedRecordType = selectedRecordType,
            temperatureText = get<String>(KEY_TEMPERATURE_TEXT).orEmpty(),
            humidityText = get<String>(KEY_HUMIDITY_TEXT).orEmpty(),
            isManualInputEnabled = get<Boolean>(KEY_MANUAL_INPUT_ENABLED) ?: false,
            workMemo = get<String>(KEY_WORK_MEMO).orEmpty(),
            restMemo = get<String>(KEY_REST_MEMO).orEmpty(),
            isTemporarilySaved = get<Boolean>(KEY_TEMPORARILY_SAVED) ?: false,
            temporarilySavedAt = get<String>(KEY_TEMPORARILY_SAVED_AT)
                ?.let { savedAt -> runCatching { OffsetDateTime.parse(savedAt) }.getOrNull() },
        )
    }

    private companion object {
        const val KEY_SELECTED_RECORD_TYPE = "record_draft.selected_record_type"
        const val KEY_TEMPERATURE_TEXT = "record_draft.temperature_text"
        const val KEY_HUMIDITY_TEXT = "record_draft.humidity_text"
        const val KEY_MANUAL_INPUT_ENABLED = "record_draft.manual_input_enabled"
        const val KEY_WORK_MEMO = "record_draft.work_memo"
        const val KEY_REST_MEMO = "record_draft.rest_memo"
        const val KEY_TEMPORARILY_SAVED = "record_draft.temporarily_saved"
        const val KEY_TEMPORARILY_SAVED_AT = "record_draft.temporarily_saved_at"
        const val UPLOAD_NOT_FOUND_CODE = "UPLOAD_NOT_FOUND"
        const val UPLOAD_ALREADY_USED_CODE = "UPLOAD_ALREADY_USED"
    }
}
