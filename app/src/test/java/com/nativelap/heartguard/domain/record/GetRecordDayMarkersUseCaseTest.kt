package com.nativelap.heartguard.domain.record

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordDayMarkersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetRecordDayMarkersUseCaseTest {
    private val firstDay = LocalDate.of(2026, 9, 1)

    @Test
    fun `a day with entries is marked as having a record`() =
        runTest {
            val repository = FakePageRepository { ApiResult.Success(RecordHistoryPage(listOf(sampleEntry()), null)) }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay))

            assertEquals(mapOf(firstDay to true), markers)
        }

    @Test
    fun `an empty first page with a next cursor is still marked as having a record`() =
        runTest {
            val repository = FakePageRepository { ApiResult.Success(RecordHistoryPage(emptyList(), "cursor-2")) }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay))

            assertEquals(mapOf(firstDay to true), markers)
        }

    @Test
    fun `an empty day without a next cursor is marked as having no record`() =
        runTest {
            val repository = FakePageRepository { ApiResult.Success(RecordHistoryPage(emptyList(), null)) }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay))

            assertEquals(mapOf(firstDay to false), markers)
        }

    @Test
    fun `a failed day is left out so it is not shown as an empty day`() =
        runTest {
            val failedDay = firstDay.plusDays(1)
            val repository =
                FakePageRepository { date ->
                    if (date == failedDay) {
                        ApiResult.Failure(ApiError.Network)
                    } else {
                        ApiResult.Success(RecordHistoryPage(listOf(sampleEntry()), null))
                    }
                }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay, failedDay))

            assertEquals(mapOf(firstDay to true), markers)
        }

    @Test
    fun `an exception other than cancellation only skips its own day`() =
        runTest {
            val brokenDay = firstDay.plusDays(1)
            val repository =
                FakePageRepository { date ->
                    if (date == brokenDay) {
                        error("broken response")
                    }
                    ApiResult.Success(RecordHistoryPage(listOf(sampleEntry()), null))
                }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay, brokenDay))

            assertEquals(mapOf(firstDay to true), markers)
        }

    @Test
    fun `cancellation is rethrown instead of being recorded as a failed day`() =
        runTest {
            val repository =
                FakePageRepository {
                    throw CancellationException("cancelled by the screen")
                }

            assertFailsWith<CancellationException> {
                GetRecordDayMarkersUseCase(repository).invoke(listOf(firstDay))
            }
        }

    @Test
    fun `at most seven day requests are in flight at the same time`() =
        runTest {
            val inFlight = AtomicInteger(0)
            val maxInFlight = AtomicInteger(0)
            val repository =
                FakePageRepository {
                    val current = inFlight.incrementAndGet()
                    maxInFlight.accumulateAndGet(current) { left, right -> maxOf(left, right) }
                    delay(10)
                    inFlight.decrementAndGet()
                    ApiResult.Success(RecordHistoryPage(emptyList(), null))
                }
            val dates = (0 until 31).map { offset -> firstDay.plusDays(offset.toLong()) }

            val markers = GetRecordDayMarkersUseCase(repository).invoke(dates)

            assertEquals(dates.size, markers.size)
            assertEquals(7, maxInFlight.get())
            assertTrue(repository.requestedDates.containsAll(dates))
        }

    private fun sampleEntry() =
        RecordHistoryEntry(
            recordId = "rec_01",
            type = FieldRecordType.THERMOMETER,
            temperature = 36.2,
            humidity = 65.0,
            apparentTemperature = 38.7,
            heatLevel = 1,
            photoCount = 0,
            photoUrls = emptyList(),
            memo = null,
            measuredAt = OffsetDateTime.parse("2026-09-01T14:02:00+09:00"),
        )

    private class FakePageRepository(
        private val getPage: suspend (LocalDate) -> ApiResult<RecordHistoryPage>,
    ) : RecordHistoryRepository {
        val requestedDates = mutableListOf<LocalDate>()

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> =
            error("Only the first page is used by the day markers")

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> {
            check(cursor == null) { "Day markers always request the first page" }
            synchronized(requestedDates) {
                requestedDates += date
            }
            return getPage(date)
        }

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            error("Record detail is not used in this test")
    }
}
