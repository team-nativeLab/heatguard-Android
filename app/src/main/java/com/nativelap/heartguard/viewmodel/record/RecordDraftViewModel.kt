package com.nativelap.heartguard.viewmodel.record

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.util.releasePhoto
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
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(RecordDraftUiState())
    val uiState: StateFlow<RecordDraftUiState> = mutableUiState.asStateFlow()

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

    fun updateFieldPhotos(photoUris: List<Uri>) {
        mutableUiState.update { it.copy(fieldPhotoUris = photoUris) }
    }

    /** 저장 전 확인 화면에서 현장 사진을 다시 찍기 전에 개별 삭제할 때 쓴다. 목록 갱신은 즉시 반영하고,
     * 파일 I/O(ContentResolver 권한 해제·캐시 파일 삭제)는 메인 스레드를 막지 않도록 백그라운드에서 한다. */
    fun removeFieldPhoto(uri: Uri) {
        mutableUiState.update { it.copy(fieldPhotoUris = it.fieldPhotoUris - uri) }
        viewModelScope.launch(ioDispatcher) {
            releasePhoto(context, uri)
        }
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

    /** 기록 유형을 새로 선택하기 시작할 때(RecordTypeSelection 진입) 호출한다.
     * 이전 시도의 입력값과, 아직 서버에 올리지 않아 로컬에만 남아 있던 임시 사진 파일을 모두 정리한다. */
    fun reset() {
        releaseAllPhotos()
        mutableUiState.value = RecordDraftUiState()
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
