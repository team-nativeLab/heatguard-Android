package com.nativelap.heartguard.viewmodel.record

import android.net.Uri

/** 기록유형 선택부터 저장 전 확인 화면까지, 화면을 오가는 동안에도 유지돼야 하는 입력값을 모은 상태다.
 * 온도계 기록과 현장 사진은 같은 "온도계" 기록 흐름에 속하고, 작업 사진·휴식 사진은 각각 독립된 흐름이다. */
data class RecordDraftUiState(
    // 수동 입력을 켜기 전까지는 화면에 보이지 않지만(TemperatureRecordScreen이 토글 상태에 따라 감춘다),
    // 토글을 켜는 순간 현재 센서 값("현재 온도" 카드와 같은 값)을 시작값으로 보여주기 위한 기본값이다.
    val temperatureText: String = "47.5",
    val humidityText: String = "55",
    val isManualInputEnabled: Boolean = false,
    // 온도계 기록 화면에서 "기록 저장"을 눌렀는지 여부. 저장 전 확인 화면과 현장 사진 화면이
    // 이 값으로 "온도계가 아직 저장이 안 되었어요" 경고 표시 여부를 판단한다.
    val isTemperatureSaved: Boolean = false,
    val fieldPhotoUris: List<Uri> = emptyList(),
    val workPhotoUris: List<Uri> = emptyList(),
    val workMemo: String = "",
    val restPhotoUris: List<Uri> = emptyList(),
    val restMemo: String = "",
    val isAlternateRestTimeSelected: Boolean = false,
)
