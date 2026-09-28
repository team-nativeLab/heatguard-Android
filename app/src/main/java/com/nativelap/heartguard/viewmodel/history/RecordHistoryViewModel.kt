package com.nativelap.heartguard.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
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
    private val getRecordHistoryUseCase: GetRecordHistoryUseCase,
    private val clock: Clock,
    sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(createDefaultUiState())
    val uiState: StateFlow<RecordHistoryUiState> = _uiState.asStateFlow()

    private var loadRecordsJob: Job? = null

    init {
        clearStateWhenSessionEnds(sessionManager) {
            loadRecordsJob?.cancel()
            loadRecordsJob = null
            _uiState.value = createDefaultUiState()
        }
    }

    /** 화면에 들어올 때 호출한다. 기간·필터는 기본값(오늘까지 최근 7일, 전체)으로 되돌리고 새로 조회한다.
     * Activity 수명 ViewModel이라 재진입 시 이전 조회 결과와 새로 저장한 기록이 어긋나지 않도록 매번 다시 받는다. */
    fun openHistory() {
        _uiState.value = createDefaultUiState()
        loadRecords()
    }

    /** 현재 기간의 기록을 다시 조회한다. 오류 화면의 재시도에서도 쓴다. */
    fun loadRecords() {
        val currentState = _uiState.value
        loadRecordsJob?.cancel()
        _uiState.value = currentState.copy(
            loadState = RecordHistoryLoadState.Loading,
        )
        loadRecordsJob = viewModelScope.launch {
            val historyResult = getRecordHistoryUseCase(
                startDate = currentState.startDate,
                endDate = currentState.endDate,
            )
            _uiState.value = _uiState.value.copy(
                loadState = when (historyResult) {
                    is ApiResult.Success -> RecordHistoryLoadState.Loaded(historyResult.value)
                    is ApiResult.Failure -> RecordHistoryLoadState.Failed
                },
            )
        }
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
