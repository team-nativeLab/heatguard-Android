package com.nativelap.heartguard.viewmodel.history

import com.nativelap.heartguard.domain.record.model.FieldRecordType

/** 기록 내역 화면의 유형 필터 칩이다. [recordType]이 null이면 전체를 뜻한다.
 * 서버 목록 API에 유형 필터가 없어 받은 기록을 화면에서 거른다. */
enum class RecordHistoryFilter(
    val recordType: FieldRecordType?,
) {
    ALL(recordType = null),
    THERMOMETER(recordType = FieldRecordType.THERMOMETER),
    WORK(recordType = FieldRecordType.WORK),
    REST(recordType = FieldRecordType.REST),
}
