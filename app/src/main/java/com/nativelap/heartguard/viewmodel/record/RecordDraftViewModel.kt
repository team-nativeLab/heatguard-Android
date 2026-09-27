package com.nativelap.heartguard.viewmodel.record

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.util.releasePhoto
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.MAX_RECORD_PHOTO_COUNT
import com.nativelap.heartguard.domain.record.usecase.SubmitFieldRecordUseCase
import com.nativelap.heartguard.domain.record.usecase.UploadFieldPhotosUseCase
import com.nativelap.heartguard.view.component.RecordType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val uploadFieldPhotosUseCase: UploadFieldPhotosUseCase,
    private val submitFieldRecordUseCase: SubmitFieldRecordUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(RecordDraftUiState())
    val uiState: StateFlow<RecordDraftUiState> = mutableUiState.asStateFlow()

    private val mutableSubmissionState = MutableStateFlow<RecordSubmissionState>(RecordSubmissionState.Idle)
    val submissionState: StateFlow<RecordSubmissionState> = mutableSubmissionState.asStateFlow()

    /** 기록유형선택 화면에서 선택을 확정할 때 호출한다. 저장 전 확인 화면의 [submit]이 어떤 종류의
     * 기록(THERMOMETER/WORK/REST)을 서버에 보낼지 이 값으로 판단한다. */
    fun selectRecordType(recordType: RecordType) {
        mutableUiState.update { it.copy(selectedRecordType = recordType) }
    }

    fun updateTemperatureText(temperatureText: String) {
        mutableUiState.update { it.copy(temperatureText = temperatureText) }
    }

    fun updateHumidityText(humidityText: String) {
        mutableUiState.update { it.copy(humidityText = humidityText) }
    }

    fun updateManualInputEnabled(isManualInputEnabled: Boolean) {
        mutableUiState.update { it.copy(isManualInputEnabled = isManualInputEnabled) }
    }

    /** 온도계 기록 화면에서 "기록 저장"을 눌러 저장 전 확인 화면으로 넘어갈 때 호출한다. */
    fun markTemperatureSaved() {
        mutableUiState.update { it.copy(isTemperatureSaved = true) }
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

    /** 저장 전 확인 화면에서 현장 사진을 다시 찍기 전에 개별 삭제할 때 쓴다. 목록 갱신은 즉시 반영하고,
     * 파일 I/O(ContentResolver 권한 해제·캐시 파일 삭제)는 메인 스레드를 막지 않도록 백그라운드에서 한다. */
    fun removeFieldPhoto(uri: Uri) {
        removePhoto(RecordType.TEMPERATURE, uri)
    }

    /** 사진 목록에서 URI를 제거하고 연결된 캐시 파일 또는 앨범 권한을 백그라운드에서 정리한다. */
    fun removePhoto(recordType: RecordType, uri: Uri) {
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
        mutableUiState.update { it.copy(workMemo = memo) }
    }

    fun updateRestMemo(memo: String) {
        mutableUiState.update { it.copy(restMemo = memo) }
    }

    fun toggleAlternateRestTime() {
        mutableUiState.update { it.copy(isAlternateRestTimeSelected = !it.isAlternateRestTimeSelected) }
    }

    /** 저장 전 확인 화면의 "저장" 버튼을 누르면 호출한다. 선택된 기록 유형에 맞는 사진을 먼저
     * presigned URL로 업로드하고, 그 objectKey로 현장 기록을 등록한다. 결과를 [submissionState]에도
     * 반영해 SaveFailure 화면이 실제 오류를 읽을 수 있게 하고, 반환값으로도 돌려줘 Route가 즉시
     * 다음 화면(성공/실패)을 결정할 수 있게 한다 — Route가 상태 변화를 구독해 내비게이션하는 대신
     * 호출 결과를 직접 받는 방식이라, 재시도로 이 화면에 되돌아왔을 때 이전 결과로 다시 자동
     * 내비게이션되는 문제가 없다. */
    suspend fun submit(): ApiResult<FieldRecord> {
        val state = mutableUiState.value
        val recordType = state.selectedRecordType
            ?: return ApiResult.Failure(ApiError.Unknown)

        mutableSubmissionState.value = RecordSubmissionState.Submitting

        val photoUris = state.photoUrisFor(recordType)
        val uploadResult = uploadFieldPhotosUseCase(photoUris)
        val photoKeys = when (uploadResult) {
            is ApiResult.Success -> uploadResult.value
            is ApiResult.Failure -> {
                mutableSubmissionState.value = RecordSubmissionState.Failure(uploadResult.error)
                return uploadResult
            }
        }

        val isManualTemperature = recordType == RecordType.TEMPERATURE && state.isManualInputEnabled
        val submitResult = submitFieldRecordUseCase(
            type = recordType.toFieldRecordType(),
            photoKeys = photoKeys,
            temperature = if (isManualTemperature) state.temperatureText.toDoubleOrNull() else null,
            humidity = if (isManualTemperature) state.humidityText.toDoubleOrNull() else null,
            noThermometer = isManualTemperature,
            memo = state.memoFor(recordType),
        )
        mutableSubmissionState.value = when (submitResult) {
            is ApiResult.Success -> RecordSubmissionState.Success(submitResult.value)
            is ApiResult.Failure -> RecordSubmissionState.Failure(submitResult.error)
        }
        return submitResult
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
        val photoUrisToRelease = currentPhotoUris()
        mutableUiState.value = RecordDraftUiState()
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
}
