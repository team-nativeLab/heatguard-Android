package com.nativelap.heartguard.data.record.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.record.mapper.toDomain
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSource
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class RecordHistoryRepositoryImpl @Inject constructor(
    private val recordHistoryRemoteDataSource: RecordHistoryRemoteDataSource,
) : RecordHistoryRepository {
    /** 하루 기록을 끝까지 이어 받는다. 커서가 반복되거나 안전 상한을 넘으면 잘린 목록을 성공으로 반환하지 않고 실패로 끝낸다. */
    override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> {
        val collectedEntries = mutableListOf<RecordHistoryEntry>()
        var nextCursor: String? = null
        val visitedCursors = mutableSetOf<String?>()

        while (true) {
            visitedCursors += nextCursor
            val recordPage = when (val pageResult = getRecordPage(date, nextCursor)) {
                is ApiResult.Success -> pageResult.value
                is ApiResult.Failure -> return pageResult
            }
            collectedEntries += recordPage.entries
            val receivedCursor = recordPage.nextCursor ?: break
            if (receivedCursor in visitedCursors || visitedCursors.size >= MAX_DAILY_PAGE_COUNT) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            nextCursor = receivedCursor
        }

        return ApiResult.Success(collectedEntries)
    }

    /** 명세상 필수인 page 정보가 없거나, 더 있다는데 다음 커서가 없으면 목록이 잘린 것이므로 실패로 바꾼다. */
    override suspend fun getRecordPage(
        date: LocalDate,
        cursor: String?,
    ): ApiResult<RecordHistoryPage> {
        val pageResult = recordHistoryRemoteDataSource.getRecordPage(
            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            cursor = cursor,
            limit = PAGE_SIZE,
        )
        val recordPage = when (pageResult) {
            is ApiResult.Success -> pageResult.value
            is ApiResult.Failure -> return pageResult
        }
        val pageInfo = recordPage.page ?: return ApiResult.Failure(ApiError.Unknown)
        val receivedCursor = pageInfo.nextCursor?.takeIf(String::isNotBlank)
        if (pageInfo.hasMore && receivedCursor == null) {
            return ApiResult.Failure(ApiError.Unknown)
        }

        return ApiResult.Success(
            RecordHistoryPage(
                entries = recordPage.items.map { recordItem -> recordItem.toDomain() },
                nextCursor = receivedCursor.takeIf { pageInfo.hasMore },
            ),
        )
    }

    override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> = recordHistoryRemoteDataSource
        .getRecordDetail(recordId)
        .map { recordDetail -> recordDetail.toDomain() }

    private companion object {
        // 명세상 limit 최대값이다. 하루 기록 수가 많지 않아 대부분 한 번에 끝난다.
        const val PAGE_SIZE = 100

        // 서버 오류로 새 커서가 끝없이 와도 홈을 열 때마다 요청이 이어지지 않게 하는 안전 상한(하루 1만 건)이다.
        const val MAX_DAILY_PAGE_COUNT = 100
    }
}
