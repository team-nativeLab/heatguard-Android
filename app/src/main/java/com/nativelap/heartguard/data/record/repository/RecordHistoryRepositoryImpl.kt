package com.nativelap.heartguard.data.record.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.mapper.toDomain
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSource
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RecordHistoryRepositoryImpl @Inject constructor(
    private val recordHistoryRemoteDataSource: RecordHistoryRemoteDataSource,
) : RecordHistoryRepository {
    /** 서버 목록은 하루 단위 커서 페이징이라, hasMore가 false가 될 때까지 다음 커서로 이어 받아 합친다.
     * 서버가 같은 커서를 반복해 무한 요청이 되지 않도록 최대 페이지 수를 둔다. 한 페이지라도 실패하면 그 실패를 돌려준다. */
    override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> {
        val requestDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val collectedItems = mutableListOf<RecordHistoryItemDto>()
        var nextCursor: String? = null
        var requestedPageCount = 0

        do {
            val pageResult = recordHistoryRemoteDataSource.getRecordPage(
                date = requestDate,
                cursor = nextCursor,
                limit = PAGE_SIZE,
            )
            val recordPage = when (pageResult) {
                is ApiResult.Success -> pageResult.value
                is ApiResult.Failure -> return pageResult
            }
            collectedItems += recordPage.items
            requestedPageCount += 1

            val hasMorePages = recordPage.page?.hasMore == true
            val receivedCursor = recordPage.page?.nextCursor
            val canRequestNextPage = hasMorePages &&
                receivedCursor != null &&
                receivedCursor != nextCursor &&
                requestedPageCount < MAX_PAGE_COUNT
            nextCursor = receivedCursor
        } while (canRequestNextPage)

        return ApiResult.Success(
            collectedItems.map { recordItem -> recordItem.toDomain() },
        )
    }

    override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> = recordHistoryRemoteDataSource
        .getRecordDetail(recordId)
        .map { recordDetail -> recordDetail.toDomain() }

    private companion object {
        // 명세상 limit 최대값이다. 하루 기록 수가 많지 않아 대부분 한 번에 끝난다.
        const val PAGE_SIZE = 100
        const val MAX_PAGE_COUNT = 20
    }
}
