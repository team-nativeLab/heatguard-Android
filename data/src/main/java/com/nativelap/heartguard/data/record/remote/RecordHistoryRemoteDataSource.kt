package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryPageDto

interface RecordHistoryRemoteDataSource {
    suspend fun getRecordPage(
        date: String,
        cursor: String?,
        limit: Int,
    ): ApiResult<RecordHistoryPageDto>

    suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryItemDto>
}
