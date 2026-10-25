package com.nativelap.heartguard.viewmodel.history

import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry

/** 기록 상세 화면의 조회 상태다. */
sealed interface RecordDetailUiState {
    data object Loading : RecordDetailUiState

    data class Loaded(
        val entry: RecordHistoryEntry,
    ) : RecordDetailUiState

    data object Failed : RecordDetailUiState
}
