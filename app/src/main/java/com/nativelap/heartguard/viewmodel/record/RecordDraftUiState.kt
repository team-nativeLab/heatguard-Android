package com.nativelap.heartguard.viewmodel.record

import android.net.Uri
import com.nativelap.heartguard.view.component.RecordType

/** 기록유형 선택부터 저장까지, 화면을 오가는 동안에도 유지돼야 하는 입력값을 모은 상태다.
 * 온도계 기록과 현장 사진은 같은 "온도계"(THERMOMETER) 기록 흐름에 속하고, 작업 사진·휴식 사진은 각각 독립된 흐름이다. */
data class RecordDraftUiState(
    // 저장할 기록의 종류(THERMOMETER/WORK/REST). 기록유형 선택 또는 홈의 현장 사진 진입 시 정해진다.
    val selectedRecordType: RecordType? = null,
    // "온도계 데이터 직접 입력"(온도계 미설치) 값. 사용자가 입력하기 전에는 비어 있고, 화면은 예시 placeholder를 보여준다.
    val temperatureText: String = "",
    val humidityText: String = "",
    val isManualInputEnabled: Boolean = false,
    val fieldPhotoUris: List<Uri> = emptyList(),
    val workPhotoUris: List<Uri> = emptyList(),
    val workMemo: String = "",
    val restPhotoUris: List<Uri> = emptyList(),
    val restMemo: String = "",
) {
    // 직접 입력한 온도·습도가 모두 숫자로 해석될 때만 true다.
    val isManualTemperatureValid: Boolean
        get() = temperatureText.toDoubleOrNull() != null &&
            humidityText.toDoubleOrNull() != null

    // 온도계가 없는 직접 입력 경로는 값과 현장 사진을 모두 요구한다. 기기 측정 경로의 측정값 유무는 Route에서 확인한다.
    val canSubmitTemperatureRecord: Boolean
        get() = if (isManualInputEnabled) {
            isManualTemperatureValid && fieldPhotoUris.isNotEmpty()
        } else {
            true
        }
}
