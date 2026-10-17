package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecordHistoryApiService {
    // 서버는 date(YYYY-MM-DD) 하루 단위 필터만 지원한다. 기간·유형 쿼리는 무시된다(2026-09-28 실서버 확인).
    @GET("api/v1/team/records")
    suspend fun getRecords(
        @Query("date") date: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): ApiEnvelope<RecordHistoryPageDto>

    @GET("api/v1/team/records/{recordId}")
    suspend fun getRecordDetail(
        @Path("recordId") recordId: String,
    ): ApiEnvelope<RecordHistoryItemDto>
}
