package com.nativelap.heartguard.viewmodel.record

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.util.releasePhoto
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.usecase.SubmitFieldRecordUseCase
import com.nativelap.heartguard.domain.record.usecase.UploadFieldPhotosUseCase
import com.nativelap.heartguard.view.component.RecordType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 기록유형선택 → 온도기록/사진촬영 → 저장 전 확인까지, 여러 NavKey가 공유해야 하는 임시 입력값 저장소다.
 * [com.nativelap.heartguard.navigation.HeartGuardNavHost]의 메인 흐름 상위에서 수동 ViewModelStoreOwner로
 * 이 인스턴스를 하나만 만들어 각 Route에 명시적으로 전달한다(android-navigation SKILL '화면 간 ViewModel 공유' 참고). */
@HiltViewModel
class RecordDraftViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
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

    fun updateTemperatureInput(
        temperatureText: String,
        humidityText: String,
        isManualInputEnabled: Boolean,
    ) {
        mutableUiState.update { current ->
            current.copy(
                temperatureText = temperatureText,
                humidityText = humidityText,
                isManualInputEnabled = isManualInputEnabled,
            )
        }
    }

    /** 온도계 기록 화면에서 "기록 저장"을 눌러 저장 전 확인 화면으로 넘어갈 때 호출한다. */
    fun markTemperatureSaved() {
        mutableUiState.update { it.copy(isTemperatureSaved = true) }
    }

    fun updateFieldPhotos(photoUris: List<Uri>) {
        mutableUiState.update { it.copy(fieldPhotoUris = photoUris) }
    }

    /** 저장 전 확인 화면에서 현장 사진을 다시 찍기 전에 개별 삭제할 때 쓴다. */
    fun removeFieldPhoto(uri: Uri) {
        releasePhoto(context, uri)
        mutableUiState.update { it.copy(fieldPhotoUris = it.fieldPhotoUris - uri) }
    }

    fun updateWorkPhotos(photoUris: List<Uri>) {
        mutableUiState.update { it.copy(workPhotoUris = photoUris) }
    }

    fun updateWorkMemo(memo: String) {
        mutableUiState.update { it.copy(workMemo = memo) }
    }

    fun updateRestPhotos(photoUris: List<Uri>) {
        mutableUiState.update { it.copy(restPhotoUris = photoUris) }
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

    /** 기록 유형을 새로 선택하기 시작할 때(RecordTypeSelection 진입) 호출한다.
     * 이전 시도의 입력값과, 아직 서버에 올리지 않아 로컬에만 남아 있던 임시 사진 파일을 모두 정리한다. */
    fun reset() {
        releaseAllPhotos()
        mutableUiState.value = RecordDraftUiState()
        mutableSubmissionState.value = RecordSubmissionState.Idle
    }

    override fun onCleared() {
        releaseAllPhotos()
    }

    private fun releaseAllPhotos() {
        val state = mutableUiState.value
        (state.fieldPhotoUris + state.workPhotoUris + state.restPhotoUris)
            .distinct()
            .forEach { uri -> releasePhoto(context, uri) }
    }
}
