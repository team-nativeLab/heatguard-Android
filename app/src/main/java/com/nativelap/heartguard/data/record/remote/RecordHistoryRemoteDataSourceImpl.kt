package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryPageDto
import javax.inject.Inject

class RecordHistoryRemoteDataSourceImpl
    @Inject
    constructor(
        private val recordHistoryApiService: RecordHistoryApiService,
        private val apiExecutor: ApiExecutor,
    ) : RecordHistoryRemoteDataSource {
        /** [date] 하루의 기록 한 페이지를 조회한다. 첫 페이지는 [cursor]를 null로 보낸다. */
        override suspend fun getRecordPage(
            date: String,
            cursor: String?,
            limit: Int,
        ): ApiResult<RecordHistoryPageDto> =
            apiExecutor
                .execute {
                    recordHistoryApiService.getRecords(
                        date = date,
                        cursor = cursor,
                        limit = limit,
                    )
                }.requireSuccessData()

        /** 기록 한 건의 상세(사진 서명 URL 포함)를 조회한다. */
        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryItemDto> =
            apiExecutor
                .execute {
                    recordHistoryApiService.getRecordDetail(recordId)
                }.requireSuccessData()
    }
