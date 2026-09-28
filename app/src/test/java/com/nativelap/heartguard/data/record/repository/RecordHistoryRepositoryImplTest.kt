package com.nativelap.heartguard.data.record.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.dto.RecordHistoryCursorDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryPageDto
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSource
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordHistoryRepositoryImplTest {

    @Test
    fun `hasMore가 true면 다음 커서로 이어 받아 하루 기록을 합친다`() = runTest {
        val remoteDataSource = FakeRecordHistoryRemoteDataSource(
            pagesByCursor = mapOf(
                null to recordPage(recordIds = listOf("rec_1", "rec_2"), nextCursor = "cur_2", hasMore = true),
                "cur_2" to recordPage(recordIds = listOf("rec_3"), nextCursor = null, hasMore = false),
            ),
        )

        val dailyResult = RecordHistoryRepositoryImpl(remoteDataSource)
            .getRecordsOfDate(LocalDate.of(2026, 9, 28))

        assertEquals(
            listOf("rec_1", "rec_2", "rec_3"),
            (dailyResult as ApiResult.Success).value.map { recordEntry -> recordEntry.recordId },
        )
        assertEquals(listOf("2026-09-28", "2026-09-28"), remoteDataSource.requestedDates)
    }

    @Test
    fun `서버가 같은 커서를 반복하면 더 요청하지 않는다`() = runTest {
        val remoteDataSource = FakeRecordHistoryRemoteDataSource(
            pagesByCursor = mapOf(
                null to recordPage(recordIds = listOf("rec_1"), nextCursor = "cur_1", hasMore = true),
                "cur_1" to recordPage(recordIds = listOf("rec_2"), nextCursor = "cur_1", hasMore = true),
            ),
        )

        RecordHistoryRepositoryImpl(remoteDataSource).getRecordsOfDate(LocalDate.of(2026, 9, 28))

        assertEquals(2, remoteDataSource.requestedDates.size)
    }

    @Test
    fun `중간 페이지가 실패하면 실패를 돌려준다`() = runTest {
        val remoteDataSource = FakeRecordHistoryRemoteDataSource(
            pagesByCursor = mapOf(
                null to recordPage(recordIds = listOf("rec_1"), nextCursor = "cur_2", hasMore = true),
            ),
        )

        val dailyResult = RecordHistoryRepositoryImpl(remoteDataSource)
            .getRecordsOfDate(LocalDate.of(2026, 9, 28))

        assertTrue(dailyResult is ApiResult.Failure)
    }

    private fun recordPage(
        recordIds: List<String>,
        nextCursor: String?,
        hasMore: Boolean,
    ) = RecordHistoryPageDto(
        items = recordIds.map { recordId -> RecordHistoryItemDto(recordId = recordId, type = "WORK") },
        page = RecordHistoryCursorDto(
            nextCursor = nextCursor,
            hasMore = hasMore,
        ),
    )

    private class FakeRecordHistoryRemoteDataSource(
        private val pagesByCursor: Map<String?, RecordHistoryPageDto>,
    ) : RecordHistoryRemoteDataSource {
        val requestedDates = mutableListOf<String>()

        override suspend fun getRecordPage(
            date: String,
            cursor: String?,
            limit: Int,
        ): ApiResult<RecordHistoryPageDto> {
            requestedDates += date
            val recordPage = pagesByCursor[cursor] ?: return ApiResult.Failure(ApiError.Network)
            return ApiResult.Success(recordPage)
        }

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryItemDto> =
            ApiResult.Failure(ApiError.Unknown)
    }
}
