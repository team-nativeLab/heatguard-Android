package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class GetRecordHistoryUseCaseTest {
    @Test
    fun `기간의 날짜마다 조회해 측정 시각 최신순으로 합친다`() =
        runTest {
            val repository =
                FakeRecordHistoryRepository(
                    failingDate = null,
                )

            val historyResult =
                GetRecordHistoryUseCase(repository)(
                    startDate = LocalDate.of(2026, 9, 26),
                    endDate = LocalDate.of(2026, 9, 28),
                )

            assertEquals(
                setOf(
                    LocalDate.of(2026, 9, 26),
                    LocalDate.of(2026, 9, 27),
                    LocalDate.of(2026, 9, 28),
                ),
                repository.requestedDates.toSet(),
            )
            assertEquals(
                listOf("rec_2026-09-28", "rec_2026-09-27", "rec_2026-09-26"),
                (historyResult as ApiResult.Success).value.map { recordEntry -> recordEntry.recordId },
            )
        }

    @Test
    fun `한 날짜라도 실패하면 실패를 돌려준다`() =
        runTest {
            val repository =
                FakeRecordHistoryRepository(
                    failingDate = LocalDate.of(2026, 9, 27),
                )

            val historyResult =
                GetRecordHistoryUseCase(repository)(
                    startDate = LocalDate.of(2026, 9, 26),
                    endDate = LocalDate.of(2026, 9, 28),
                )

            assertTrue(historyResult is ApiResult.Failure)
        }

    @Test(expected = IllegalArgumentException::class)
    fun `최대 기간을 넘으면 요청하지 않는다`() =
        runTest {
            GetRecordHistoryUseCase(FakeRecordHistoryRepository(failingDate = null))(
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 9, 28),
            )
        }

    private class FakeRecordHistoryRepository(
        private val failingDate: LocalDate?,
    ) : RecordHistoryRepository {
        val requestedDates = mutableListOf<LocalDate>()

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> {
            requestedDates += date
            if (date == failingDate) {
                return ApiResult.Failure(ApiError.Network)
            }
            return ApiResult.Success(
                listOf(
                    RecordHistoryEntry(
                        recordId = "rec_$date",
                        type = FieldRecordType.WORK,
                        temperature = null,
                        humidity = null,
                        apparentTemperature = null,
                        heatLevel = null,
                        photoCount = 1,
                        photoUrls = emptyList(),
                        memo = null,
                        measuredAt = OffsetDateTime.of(date.atTime(9, 0), ZoneOffset.ofHours(9)),
                    ),
                ),
            )
        }

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> = ApiResult.Failure(ApiError.Unknown)

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            ApiResult.Failure(ApiError.Unknown)
    }
}
