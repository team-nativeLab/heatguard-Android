package com.nativelap.heartguard.domain.record.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import java.time.LocalDate

interface RecordHistoryRepository {
    /** [date] 하루(Asia/Seoul 기준)에 저장된 기록 전체를 돌려준다. 여러 페이지면 모두 이어 받는다. */
    suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>>

    suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry>
}
