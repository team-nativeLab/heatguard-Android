package com.nativelap.heartguard.viewmodel.history

import java.time.LocalDate

/** 기록 내역 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface RecordHistoryScreenEvent {
    data object BackClicked : RecordHistoryScreenEvent

    // 날짜 카드의 "2026년 9월" 영역을 눌러 날짜 선택 시트를 연다.
    data object DatePickerClicked : RecordHistoryScreenEvent

    // 주간 스트립의 날짜를 누르거나 날짜 시트에서 "기록 보기"를 눌러 날짜를 확정한다.
    data class DateSelected(
        val date: LocalDate,
    ) : RecordHistoryScreenEvent

    data object DatePickerDismissed : RecordHistoryScreenEvent

    data class MonthMoved(
        val monthDelta: Int,
    ) : RecordHistoryScreenEvent

    data object TodayClicked : RecordHistoryScreenEvent

    data class FilterSelected(
        val filter: RecordHistoryFilter,
    ) : RecordHistoryScreenEvent

    data class RecordClicked(
        val recordId: String,
    ) : RecordHistoryScreenEvent

    data object TemporaryDraftClicked : RecordHistoryScreenEvent

    data object PendingSubmissionsRetryClicked : RecordHistoryScreenEvent

    data object RetryClicked : RecordHistoryScreenEvent

    // 목록 끝에 닿았거나 이어 받기 실패 후 재시도를 눌렀을 때 다음 기록을 요청한다.
    data object LoadMore : RecordHistoryScreenEvent

    data object CreateRecordClicked : RecordHistoryScreenEvent
}
