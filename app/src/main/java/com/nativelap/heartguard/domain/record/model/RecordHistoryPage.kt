package com.nativelap.heartguard.domain.record.model

/** 하루 기록 목록의 한 페이지다. [nextCursor]가 null이면 그 날의 마지막 페이지다. */
data class RecordHistoryPage(
    val entries: List<RecordHistoryEntry>,
    val nextCursor: String?,
)
