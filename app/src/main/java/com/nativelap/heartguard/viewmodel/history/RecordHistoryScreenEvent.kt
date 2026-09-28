package com.nativelap.heartguard.viewmodel.history

/** 기록 내역 화면에서 발생하는 사용자 의도를 Route에 전달한다. */
sealed interface RecordHistoryScreenEvent {
    data object BackClicked : RecordHistoryScreenEvent
}
