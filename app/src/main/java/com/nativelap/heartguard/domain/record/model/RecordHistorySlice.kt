package com.nativelap.heartguard.domain.record.model

import java.time.LocalDate

/** 기간 기록을 최신 날짜부터 이어 받을 다음 요청 위치다. [cursor]가 null이면 [date]의 첫 페이지다. */
data class RecordHistoryPosition(
    val date: LocalDate,
    val cursor: String?,
)

/** 기간 기록을 이어 받은 한 번의 결과다. [nextPosition]이 null이면 기간 끝까지 받았다. */
data class RecordHistorySlice(
    val entries: List<RecordHistoryEntry>,
    val nextPosition: RecordHistoryPosition?,
)
