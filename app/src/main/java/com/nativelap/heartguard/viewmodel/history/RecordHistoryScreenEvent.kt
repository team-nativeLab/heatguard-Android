package com.nativelap.heartguard.viewmodel.history

import java.time.LocalDate

/** 기록 내역 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface RecordHistoryScreenEvent {
    data object BackClicked : RecordHistoryScreenEvent

    data object DateRangeClicked : RecordHistoryScreenEvent

    data class DateRangeSelected(
        val startDate: LocalDate,
        val endDate: LocalDate,
    ) : RecordHistoryScreenEvent

    data object DateRangeDismissed : RecordHistoryScreenEvent

    data class FilterSelected(val filter: RecordHistoryFilter) : RecordHistoryScreenEvent

    data class RecordClicked(val recordId: String) : RecordHistoryScreenEvent

    data object TemporaryDraftClicked : RecordHistoryScreenEvent

    data object PendingSubmissionsRetryClicked : RecordHistoryScreenEvent

    data object RetryClicked : RecordHistoryScreenEvent

    // 목록 끝에 닿았거나 이어 받기 실패 후 재시도를 눌렀을 때 다음 기록을 요청한다.
    data object LoadMore : RecordHistoryScreenEvent

    data object CreateRecordClicked : RecordHistoryScreenEvent
}
