package com.nativelap.heartguard.data.record.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.dto.RecordHistoryCursorDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryItemDto
import com.nativelap.heartguard.data.record.dto.RecordHistoryPageDto
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecordHistoryRepositoryImplTest {
    @Test
    fun `hasMore가 true면 다음 커서로 이어 받아 하루 기록을 합친다`() =
        runTest {
            val remoteDataSource =
                FakeRecordHistoryRemoteDataSource(
                    pagesByCursor =
                        mapOf(
                            null to
                                recordPage(recordIds = listOf("rec_1", "rec_2"), nextCursor = "cur_2", hasMore = true),
                            "cur_2" to recordPage(recordIds = listOf("rec_3"), nextCursor = null, hasMore = false),
                        ),
                )

            val dailyResult =
                RecordHistoryRepositoryImpl(remoteDataSource)
                    .getRecordsOfDate(LocalDate.of(2026, 9, 28))

            assertEquals(
                listOf("rec_1", "rec_2", "rec_3"),
                (dailyResult as ApiResult.Success).value.map { recordEntry -> recordEntry.recordId },
            )
            assertEquals(listOf("2026-09-28", "2026-09-28"), remoteDataSource.requestedDates)
        }

    @Test
    fun `서버가 같은 커서를 반복하면 더 요청하지 않는다`() =
        runTest {
            val remoteDataSource =
                FakeRecordHistoryRemoteDataSource(
                    pagesByCursor =
                        mapOf(
                            null to recordPage(recordIds = listOf("rec_1"), nextCursor = "cur_1", hasMore = true),
                            "cur_1" to recordPage(recordIds = listOf("rec_2"), nextCursor = "cur_1", hasMore = true),
                        ),
                )

            val dailyResult = RecordHistoryRepositoryImpl(remoteDataSource).getRecordsOfDate(LocalDate.of(2026, 9, 28))

            assertEquals(2, remoteDataSource.requestedDates.size)
            assertEquals(ApiResult.Failure(ApiError.Unknown), dailyResult)
        }

    @Test
    fun `중간 페이지가 실패하면 실패를 돌려준다`() =
        runTest {
            val remoteDataSource =
                FakeRecordHistoryRemoteDataSource(
                    pagesByCursor =
                        mapOf(
                            null to recordPage(recordIds = listOf("rec_1"), nextCursor = "cur_2", hasMore = true),
                        ),
                )

            val dailyResult =
                RecordHistoryRepositoryImpl(remoteDataSource)
                    .getRecordsOfDate(LocalDate.of(2026, 9, 28))

            assertTrue(dailyResult is ApiResult.Failure)
        }

    @Test
    fun manyPagesAreLoadedWithoutPageCountLimit() =
        runTest {
            val source =
                FakeRecordHistoryRemoteDataSource(
                    (0 until 25).associate { pageIndex ->
                        (if (pageIndex == 0) null else "cur_$pageIndex") to
                            recordPage(listOf("rec_$pageIndex"), "cur_${pageIndex + 1}", pageIndex != 24)
                    },
                )
            val dailyResult = RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28))
            assertEquals(25, (dailyResult as ApiResult.Success).value.size)
            assertEquals(25, source.requestedDates.size)
        }

    @Test
    fun endlessNewCursorsStopAtSafetyLimitAsFailure() =
        runTest {
            val source =
                FakeRecordHistoryRemoteDataSource(
                    (0 until 150).associate { pageIndex ->
                        (if (pageIndex == 0) null else "cur_$pageIndex") to
                            recordPage(listOf("rec_$pageIndex"), "cur_${pageIndex + 1}", true)
                    },
                )
            val dailyResult = RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28))
            assertEquals(ApiResult.Failure(ApiError.Unknown), dailyResult)
            assertEquals(100, source.requestedDates.size)
        }

    @Test
    fun singlePageKeepsNextCursorOnlyWhenMoreRecordsRemain() =
        runTest {
            val source =
                FakeRecordHistoryRemoteDataSource(
                    mapOf(
                        null to recordPage(listOf("rec_0"), "cur_1", true),
                        "cur_1" to recordPage(listOf("rec_1"), "stale_cursor", false),
                    ),
                )
            val repository = RecordHistoryRepositoryImpl(source)
            val firstPage = repository.getRecordPage(LocalDate.of(2026, 9, 28), cursor = null)
            val lastPage = repository.getRecordPage(LocalDate.of(2026, 9, 28), cursor = "cur_1")

            assertEquals("cur_1", (firstPage as ApiResult.Success).value.nextCursor)
            assertEquals(null, (lastPage as ApiResult.Success).value.nextCursor)
        }

    @Test
    fun multiStepCursorCycleFailsBeforeRepeatedRequest() =
        runTest {
            val source =
                FakeRecordHistoryRemoteDataSource(
                    mapOf(
                        null to recordPage(listOf("rec_0"), "cur_1", true),
                        "cur_1" to recordPage(listOf("rec_1"), "cur_2", true),
                        "cur_2" to recordPage(listOf("rec_2"), "cur_1", true),
                    ),
                )
            assertEquals(
                ApiResult.Failure(ApiError.Unknown),
                RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28)),
            )
            assertEquals(3, source.requestedDates.size)
        }

    @Test
    fun missingOrBlankContinuationCursorIsFailure() =
        runTest {
            listOf(null, "", " ").forEach { cursor ->
                val source = FakeRecordHistoryRemoteDataSource(mapOf(null to recordPage(listOf("rec_0"), cursor, true)))
                assertEquals(
                    ApiResult.Failure(ApiError.Unknown),
                    RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28)),
                )
                assertEquals(1, source.requestedDates.size)
            }
        }

    @Test
    fun pageWithoutPaginationInfoIsFailure() =
        runTest {
            val source =
                FakeRecordHistoryRemoteDataSource(
                    mapOf(null to RecordHistoryPageDto(items = listOf(RecordHistoryItemDto("rec_0", "WORK")))),
                )
            val dailyResult = RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28))
            assertEquals(ApiResult.Failure(ApiError.Unknown), dailyResult)
        }

    @Test
    fun cancellationIsPropagated() =
        runTest {
            val source = FakeRecordHistoryRemoteDataSource(emptyMap(), cancelRequests = true)
            try {
                RecordHistoryRepositoryImpl(source).getRecordsOfDate(LocalDate.of(2026, 9, 28))
                throw AssertionError("Cancellation must propagate")
            } catch (_: CancellationException) {
                assertEquals(1, source.requestedDates.size)
            }
        }

    private fun recordPage(
        recordIds: List<String>,
        nextCursor: String?,
        hasMore: Boolean,
    ) = RecordHistoryPageDto(
        items = recordIds.map { recordId -> RecordHistoryItemDto(recordId = recordId, type = "WORK") },
        page =
            RecordHistoryCursorDto(
                nextCursor = nextCursor,
                hasMore = hasMore,
            ),
    )

    private class FakeRecordHistoryRemoteDataSource(
        private val pagesByCursor: Map<String?, RecordHistoryPageDto>,
        private val cancelRequests: Boolean = false,
    ) : RecordHistoryRemoteDataSource {
        val requestedDates = mutableListOf<String>()

        override suspend fun getRecordPage(
            date: String,
            cursor: String?,
            limit: Int,
        ): ApiResult<RecordHistoryPageDto> {
            requestedDates += date
            if (cancelRequests) {
                throw CancellationException("Request cancelled")
            }
            val recordPage = pagesByCursor[cursor] ?: return ApiResult.Failure(ApiError.Network)
            return ApiResult.Success(recordPage)
        }

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryItemDto> =
            ApiResult.Failure(ApiError.Unknown)
    }
}
