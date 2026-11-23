package com.nativelap.heartguard.viewmodel.history

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPage
import com.nativelap.heartguard.domain.record.model.RecordHistoryPosition
import com.nativelap.heartguard.domain.record.model.RecordHistorySlice
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import com.nativelap.heartguard.domain.record.usecase.GetRecordDayMarkersUseCase
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistorySliceUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth

class RecordHistoryOptimizationTest {
    private val today = LocalDate.of(2026, 11, 23)

    @Test
    fun selectedDateIsExcludedOnlyWhileFirstPageIsLoading() {
        val loading = RecordHistoryUiState(today, today, YearMonth.from(today))
        assertFalse(today in loading.markerDatesToRequest(emptySet()))
        assertTrue(today in loading.copy(loadState = RecordHistoryLoadState.Failed).markerDatesToRequest(emptySet()))
        assertFalse(today in loading.copy(loadState = RecordHistoryLoadState.Failed).markerDatesToRequest(setOf(today)))
    }

    @Test
    fun emptyFirstPageWithCursorStillHasRecords() {
        val position = RecordHistoryPosition(date = today, cursor = "next")
        assertTrue(RecordHistorySlice(emptyList(), position).hasRecordsForSelectedDate())
        assertFalse(RecordHistorySlice(emptyList(), null).hasRecordsForSelectedDate())
    }

    @Test
    fun cachedGroupsAndCountsPreserveFilteringOrderAndUnknownTypes() {
        val later = entry("later", FieldRecordType.WORK, "2026-11-23T12:00:00+09:00")
        val earlier = entry("earlier", FieldRecordType.WORK, "2026-11-23T08:00:00+09:00")
        val unknown = entry("unknown", null, null)
        val state =
            RecordHistoryUiState(
                today = today,
                selectedDate = today,
                visibleMonth = YearMonth.from(today),
                loadState = RecordHistoryLoadState.Loaded(listOf(earlier, unknown, later)),
            )
        assertEquals(RecordHistoryCounts(total = 3, thermometer = 0, work = 2, rest = 0), state.recordCounts)
        assertEquals(listOf("later", "earlier", "unknown"), state.dayGroups.flatMap { it.entries }.map { it.recordId })
        val filtered = state.copy(selectedFilter = RecordHistoryFilter.WORK)
        assertEquals(listOf("later", "earlier"), filtered.dayGroups.flatMap { it.entries }.map { it.recordId })
        assertEquals(3, filtered.recordCounts.total)
        assertEquals(0, filtered.copy(loadState = RecordHistoryLoadState.Loaded(emptyList())).recordCounts.total)
    }

    @Test
    fun firstPageAndMarkerPlanDoNotDuplicateSelectedDateRequest() =
        runBlocking {
            val selected = LocalDate.of(2026, 11, 25)
            val state = RecordHistoryUiState(selected, selected, YearMonth.from(selected))
            val repository = CountingRepository()
            val slice = GetRecordHistorySliceUseCase(repository)(selected, RecordHistoryPosition(selected, null))
            val markers = GetRecordDayMarkersUseCase(repository)(state.markerDatesToRequest(emptySet()))

            assertTrue(slice is ApiResult.Success)
            assertEquals(1, repository.calls.count { it == selected })
            assertFalse(selected in markers)
            assertEquals(3, repository.calls.size)
        }

    @Test
    fun failedFirstPageRemainsEligibleForMarkerRetry() =
        runBlocking {
            val state = RecordHistoryUiState(today, today, YearMonth.from(today))
            val repository = CountingRepository(failFirst = true)
            val first = GetRecordHistorySliceUseCase(repository)(today, RecordHistoryPosition(today, null))
            assertTrue(first is ApiResult.Failure)
            val markers =
                GetRecordDayMarkersUseCase(repository)(
                    state.copy(loadState = RecordHistoryLoadState.Failed).markerDatesToRequest(emptySet()),
                )
            assertEquals(false, markers[today])
            assertEquals(2, repository.calls.count { it == today })
        }

    private class CountingRepository(
        private var failFirst: Boolean = false,
    ) : RecordHistoryRepository {
        val calls = mutableListOf<LocalDate>()

        override suspend fun getRecordPage(
            date: LocalDate,
            cursor: String?,
        ): ApiResult<RecordHistoryPage> {
            calls += date
            if (failFirst) {
                failFirst = false
                return ApiResult.Failure(ApiError.Network)
            }
            return ApiResult.Success(RecordHistoryPage(emptyList(), null))
        }

        override suspend fun getRecordsOfDate(date: LocalDate): ApiResult<List<RecordHistoryEntry>> =
            error("Unexpected request")

        override suspend fun getRecordDetail(recordId: String): ApiResult<RecordHistoryEntry> =
            error("Unexpected request")
    }

    private fun entry(
        id: String,
        type: FieldRecordType?,
        measuredAt: String?,
    ) = RecordHistoryEntry(
        recordId = id,
        type = type,
        temperature = null,
        humidity = null,
        apparentTemperature = null,
        heatLevel = null,
        photoCount = 0,
        photoUrls = emptyList(),
        memo = null,
        measuredAt = measuredAt?.let(OffsetDateTime::parse),
    )
}
