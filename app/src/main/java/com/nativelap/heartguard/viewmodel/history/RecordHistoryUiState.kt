package com.nativelap.heartguard.viewmodel.history

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** 기록 내역 화면 상태다. 선택한 날짜와 서버 조회 결과, 날짜 선택 시트 표시 상태를 가진다. */
@Immutable
data class RecordHistoryUiState(
    // 날짜 기본값과 "오늘·어제" 표시, 선택 가능 범위의 기준 날짜(Asia/Seoul)다.
    val today: LocalDate,
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
    val selectedFilter: RecordHistoryFilter = RecordHistoryFilter.ALL,
    val loadState: RecordHistoryLoadState = RecordHistoryLoadState.Loading,
    val isDatePickerVisible: Boolean = false,
    // 날짜별 기록 유무다. 조회에 실패한 날짜는 키가 없어 '기록 없음'과 구분한다.
    val recordDayMarkers: Map<LocalDate, Boolean> = emptyMap(),
) {
    private val loadedEntries: List<RecordHistoryEntry>
        get() = (loadState as? RecordHistoryLoadState.Loaded)?.entries.orEmpty()

    private val loadedState: RecordHistoryLoadState.Loaded?
        get() = loadState as? RecordHistoryLoadState.Loaded

    /** 선택한 날짜에 아직 받지 않은 기록이 남았는지다. 스크롤 끝에서 이어 받을지 판단한다. */
    val hasMore: Boolean
        get() = loadedState?.nextPosition != null

    val isLoadingMore: Boolean
        get() = loadedState?.isLoadingMore == true

    val hasLoadMoreError: Boolean
        get() = loadedState?.hasLoadMoreError == true

    /** 지금까지 불러온 기록의 유형별 건수 요약이다. 앱이 모르는 유형은 전체 건수에만 포함된다. */
    val recordCounts: RecordHistoryCounts
        get() =
            RecordHistoryCounts(
                total = loadedEntries.size,
                thermometer = loadedEntries.count { recordEntry -> recordEntry.type == FieldRecordType.THERMOMETER },
                work = loadedEntries.count { recordEntry -> recordEntry.type == FieldRecordType.WORK },
                rest = loadedEntries.count { recordEntry -> recordEntry.type == FieldRecordType.REST },
            )

    /** 선택한 필터를 적용해 측정 날짜별(최신 날짜 먼저)로 묶은 목록이다. 측정 시각이 없는 기록은 제외하지 않고 맨 뒤 그룹에 둔다. */
    val dayGroups: List<RecordHistoryDayGroup>
        get() =
            loadedEntries
                .filter { recordEntry ->
                    selectedFilter.recordType == null || recordEntry.type == selectedFilter.recordType
                }.groupBy { recordEntry -> recordEntry.measuredAt?.toLocalDate() }
                .map { (measuredDate, dayEntries) ->
                    RecordHistoryDayGroup(
                        date = measuredDate,
                        // 같은 날 기록이 여러 페이지로 나뉘어 와도 최신 측정 시각이 위로 오게 한다.
                        entries = dayEntries.sortedByDescending { recordEntry -> recordEntry.measuredAt },
                    )
                }.sortedWith(
                    compareByDescending<RecordHistoryDayGroup> { dayGroup -> dayGroup.date != null }
                        .thenByDescending { dayGroup -> dayGroup.date },
                )

    /** 달력 시트에 보이는 달의 모든 날짜다. */
    val visibleMonthDates: List<LocalDate>
        get() = (1..visibleMonth.lengthOfMonth()).map { day -> visibleMonth.atDay(day) }

    /** 선택한 날짜가 속한 주(월요일~일요일) 날짜다. 주간 스트립에 쓴다. */
    val selectedWeekDates: List<LocalDate>
        get() {
            val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return (0L..6L).map { offset -> monday.plusDays(offset) }
        }

    /** 기록 유무를 조회할 날짜다. 날짜 시트가 열려 있으면 보이는 달과 선택한 주의 합집합을, 닫혀 있으면 선택한 주만 담는다. 오늘 이후 날짜는 뺀다. */
    val recordMarkerDates: Set<LocalDate>
        get() {
            val candidateDates =
                if (isDatePickerVisible) {
                    visibleMonthDates + selectedWeekDates
                } else {
                    selectedWeekDates
                }
            return candidateDates
                .filterNot { date -> date.isAfter(today) }
                .toSet()
        }
}

/** 기록 조회 결과다. */
sealed interface RecordHistoryLoadState {
    data object Loading : RecordHistoryLoadState

    /** [nextPosition]이 null이면 선택한 날짜의 기록을 끝까지 받았다. 이어 받기 실패는 받은 목록을 유지한 채 [hasLoadMoreError]로 알린다. */
    data class Loaded(
        val entries: List<RecordHistoryEntry>,
        val nextPosition: RecordHistoryPosition? = null,
        val isLoadingMore: Boolean = false,
        val hasLoadMoreError: Boolean = false,
    ) : RecordHistoryLoadState

    data object Failed : RecordHistoryLoadState
}

@Immutable
data class RecordHistoryCounts(
    val total: Int,
    val thermometer: Int,
    val work: Int,
    val rest: Int,
)

/** 같은 날 측정한 기록 묶음이다. [date]가 null이면 측정 시각을 알 수 없는 기록이다. */
@Immutable
data class RecordHistoryDayGroup(
    val date: LocalDate?,
    val entries: List<RecordHistoryEntry>,
)
