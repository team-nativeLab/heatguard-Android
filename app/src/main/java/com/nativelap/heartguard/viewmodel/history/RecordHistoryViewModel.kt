package com.nativelap.heartguard.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPosition
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistorySliceUseCase
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 기록 내역 화면의 기간 선택·유형 필터·서버 조회를 담당한다. */
@HiltViewModel
class RecordHistoryViewModel @Inject constructor(
    private val getRecordHistorySliceUseCase: GetRecordHistorySliceUseCase,
    private val clock: Clock,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(createDefaultUiState())
    val uiState: StateFlow<RecordHistoryUiState> = _uiState.asStateFlow()

    private var loadRecordsJob: Job? = null

    // 같은 조회 조건에서 이미 받은 위치다. 서버 커서가 되돌아오면 무한 반복 대신 목록을 끝내고 실패를 알린다.
    private val loadedPositions = mutableSetOf<RecordHistoryPosition>()

    init {
        clearStateWhenSessionEnds(sessionManager) {
            loadRecordsJob?.cancel()
            loadRecordsJob = null
            loadedPositions.clear()
            _uiState.value = createDefaultUiState()
        }
    }

    /** 화면에 들어올 때 호출한다. 기간·필터는 기본값(오늘까지 최근 7일, 전체)으로 되돌리고 새로 조회한다.
     * Activity 수명 ViewModel이라 재진입 시 이전 조회 결과와 새로 저장한 기록이 어긋나지 않도록 매번 다시 받는다. */
    fun openHistory() {
        _uiState.value = createDefaultUiState()
        loadRecords()
    }

    /** 현재 기간의 첫 기록을 다시 조회한다. 오류 화면의 재시도와 기간 변경에서도 쓴다. */
    fun loadRecords() {
        val currentState = _uiState.value
        loadRecordsJob?.cancel()
        loadedPositions.clear()
        _uiState.value = currentState.copy(
            loadState = RecordHistoryLoadState.Loading,
        )
        requestSlice(
            startDate = currentState.startDate,
            position = RecordHistoryPosition(
                date = currentState.endDate,
                cursor = null,
            ),
            previousEntries = null,
        )
    }

    /** 목록 끝에 닿았을 때 다음 기록을 이어 받는다. 이어 받기 실패 뒤의 재시도에서도 쓴다. */
    fun loadMoreRecords() {
        val loadedState = _uiState.value.loadState as? RecordHistoryLoadState.Loaded ?: return
        val nextPosition = loadedState.nextPosition ?: return
        if (loadedState.isLoadingMore) {
            return
        }
        _uiState.value = _uiState.value.copy(
            loadState = loadedState.copy(
                isLoadingMore = true,
                hasLoadMoreError = false,
            ),
        )
        requestSlice(
            startDate = _uiState.value.startDate,
            position = nextPosition,
            previousEntries = loadedState.entries,
        )
    }

    private fun requestSlice(
        startDate: LocalDate,
        position: RecordHistoryPosition,
        previousEntries: List<RecordHistoryEntry>?,
    ) {
        val owningGeneration = sessionManager.getSnapshot().generation
        loadRecordsJob = viewModelScope.launch {
            val sliceResult = getRecordHistorySliceUseCase(
                startDate = startDate,
                position = position,
            )
            // 여러 날짜를 이어 받는 동안 계정이 바뀌었으면 이전 계정 기록을 섞지 않는다.
            if (owningGeneration != sessionManager.getSnapshot().generation) {
                return@launch
            }
            when (sliceResult) {
                is ApiResult.Success -> {
                    val receivedSlice = sliceResult.value
                    loadedPositions += position
                    val receivedNextPosition = receivedSlice.nextPosition
                    val isRepeatedPosition = receivedNextPosition != null && receivedNextPosition in loadedPositions
                    _uiState.value = _uiState.value.copy(
                        loadState = RecordHistoryLoadState.Loaded(
                            entries = (previousEntries.orEmpty() + receivedSlice.entries)
                                .distinctBy { recordEntry -> recordEntry.recordId },
                            nextPosition = receivedNextPosition.takeUnless { isRepeatedPosition },
                            hasLoadMoreError = isRepeatedPosition,
                        ),
                    )
                }

                is ApiResult.Failure -> applySliceFailure(previousEntries)
            }
        }
    }

    private fun applySliceFailure(
        previousEntries: List<RecordHistoryEntry>?,
    ) {
        val loadedState = _uiState.value.loadState as? RecordHistoryLoadState.Loaded
        _uiState.value = _uiState.value.copy(
            loadState = if (previousEntries == null || loadedState == null) {
                RecordHistoryLoadState.Failed
            } else {
                loadedState.copy(
                    isLoadingMore = false,
                    hasLoadMoreError = true,
                )
            },
        )
    }

    fun showDateRangePicker() {
        _uiState.value = _uiState.value.copy(isDateRangePickerVisible = true)
    }

    fun hideDateRangePicker() {
        _uiState.value = _uiState.value.copy(isDateRangePickerVisible = false)
    }

    /** 기간을 바꾸고 다시 조회한다. 서버가 하루 단위 조회만 지원해 기간이 길면 요청이 많아지므로
     * [GetRecordHistoryUseCase.MAX_RANGE_DAYS]일을 넘으면 종료일 기준으로 잘라 조회한다. 오늘 이후 날짜도 오늘로 맞춘다. */
    fun selectDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        val today = _uiState.value.today
        val adjustedEndDate = minOf(maxOf(startDate, endDate), today)
        val earliestStartDate = adjustedEndDate.minusDays(GetRecordHistoryUseCase.MAX_RANGE_DAYS - 1L)
        val adjustedStartDate = maxOf(minOf(startDate, adjustedEndDate), earliestStartDate)

        _uiState.value = _uiState.value.copy(
            startDate = adjustedStartDate,
            endDate = adjustedEndDate,
            isDateRangePickerVisible = false,
        )
        loadRecords()
    }

    /** 유형 필터만 바꾼다. 이미 받은 목록을 화면에서 거르므로 서버를 다시 호출하지 않는다. */
    fun selectFilter(filter: RecordHistoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    private fun createDefaultUiState(): RecordHistoryUiState {
        val today = LocalDate.now(clock)
        return RecordHistoryUiState(
            today = today,
            startDate = today.minusDays(DEFAULT_RANGE_DAYS - 1L),
            endDate = today,
        )
    }

    private companion object {
        // Figma 20_기록내역_목록 기본 기간(최근 7일)이다.
        const val DEFAULT_RANGE_DAYS = 7
    }
}
