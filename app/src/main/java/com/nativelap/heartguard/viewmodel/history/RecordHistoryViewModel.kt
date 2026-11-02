package com.nativelap.heartguard.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.model.RecordHistoryPosition
import com.nativelap.heartguard.domain.record.usecase.GetRecordDayMarkersUseCase
import com.nativelap.heartguard.domain.record.usecase.GetRecordHistorySliceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** 기록 내역 화면의 날짜 선택·유형 필터·서버 조회와 날짜별 기록 점 조회를 담당한다. */
@HiltViewModel
class RecordHistoryViewModel
    @Inject
    constructor(
        private val getRecordHistorySliceUseCase: GetRecordHistorySliceUseCase,
        private val getRecordDayMarkersUseCase: GetRecordDayMarkersUseCase,
        private val clock: Clock,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(createDefaultUiState())
        val uiState: StateFlow<RecordHistoryUiState> = _uiState.asStateFlow()

        private var loadRecordsJob: Job? = null

        // 같은 조회 조건에서 이미 받은 위치다. 서버 커서가 되돌아오면 무한 반복 대신 목록을 끝내고 실패를 알린다.
        private val loadedPositions = mutableSetOf<RecordHistoryPosition>()

        // 날짜별 기록 유무 캐시다. 조회에 실패한 날짜는 넣지 않아 다음에 보일 때 다시 조회한다.
        private val recordDayMarkers = mutableMapOf<LocalDate, Boolean>()
        private var markerJob: Job? = null
        private var requestingMarkerDates: Set<LocalDate> = emptySet()

        init {
            clearStateWhenSessionEnds(sessionManager) {
                loadRecordsJob?.cancel()
                loadRecordsJob = null
                loadedPositions.clear()
                clearRecordDayMarkers()
                _uiState.value = createDefaultUiState()
            }
        }

        /** 기록 조회를 한 번이라도 시작했는지다. 프로세스 복원으로 새로 만들어진 ViewModel을 구분하는 데 쓴다. */
        val hasStartedHistory: Boolean
            get() = loadRecordsJob != null

        /** 기록 내역 화면을 처음 여는 진입에서 호출한다. 날짜·필터는 기본값(오늘, 전체)으로 되돌리고, 기록과 선택한 주의 날짜별 점을 새로 조회한다.
         * 점 캐시와 진행 중인 점 요청은 비운다. 보이는 달의 점은 날짜 시트를 열 때 요청한다.
         * 상세 화면에서 돌아오는 복귀는 Route가 이 함수를 다시 부르지 않아 날짜·필터·받은 목록을 그대로 둔다(Route 가드 참고). */
        fun openHistory() {
            clearRecordDayMarkers()
            _uiState.value = createDefaultUiState()
            loadRecords()
            requestRecordDayMarkers()
        }

        /** 선택한 날짜의 첫 기록을 다시 조회한다. 오류 화면의 재시도와 날짜 변경에서도 쓴다. */
        fun loadRecords() {
            val currentState = _uiState.value
            loadRecordsJob?.cancel()
            loadedPositions.clear()
            _uiState.value =
                currentState.copy(
                    loadState = RecordHistoryLoadState.Loading,
                )
            requestSlice(
                selectedDate = currentState.selectedDate,
                position =
                    RecordHistoryPosition(
                        date = currentState.selectedDate,
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
            _uiState.value =
                _uiState.value.copy(
                    loadState =
                        loadedState.copy(
                            isLoadingMore = true,
                            hasLoadMoreError = false,
                        ),
                )
            requestSlice(
                selectedDate = _uiState.value.selectedDate,
                position = nextPosition,
                previousEntries = loadedState.entries,
            )
        }

        private fun requestSlice(
            selectedDate: LocalDate,
            position: RecordHistoryPosition,
            previousEntries: List<RecordHistoryEntry>?,
        ) {
            val owningGeneration = sessionManager.getSnapshot().generation
            loadRecordsJob =
                viewModelScope.launch {
                    val sliceResult =
                        getRecordHistorySliceUseCase(
                            startDate = selectedDate,
                            position = position,
                        )
                    // 여러 페이지를 이어 받는 동안 계정이 바뀌었으면 이전 계정 기록을 섞지 않는다.
                    if (owningGeneration != sessionManager.getSnapshot().generation) {
                        return@launch
                    }
                    when (sliceResult) {
                        is ApiResult.Success -> {
                            val receivedSlice = sliceResult.value
                            loadedPositions += position
                            val receivedNextPosition = receivedSlice.nextPosition
                            val isRepeatedPosition =
                                receivedNextPosition != null && receivedNextPosition in loadedPositions
                            _uiState.value =
                                _uiState.value.copy(
                                    loadState =
                                        RecordHistoryLoadState.Loaded(
                                            entries =
                                                (previousEntries.orEmpty() + receivedSlice.entries)
                                                    .distinctBy { recordEntry -> recordEntry.recordId },
                                            nextPosition = receivedNextPosition.takeUnless { isRepeatedPosition },
                                            hasLoadMoreError = isRepeatedPosition,
                                        ),
                                )
                        }

                        is ApiResult.Failure -> {
                            applySliceFailure(previousEntries)
                        }
                    }
                }
        }

        private fun applySliceFailure(previousEntries: List<RecordHistoryEntry>?) {
            val loadedState = _uiState.value.loadState as? RecordHistoryLoadState.Loaded
            _uiState.value =
                _uiState.value.copy(
                    loadState =
                        if (previousEntries == null || loadedState == null) {
                            RecordHistoryLoadState.Failed
                        } else {
                            loadedState.copy(
                                isLoadingMore = false,
                                hasLoadMoreError = true,
                            )
                        },
                )
        }

        /** 날짜를 골라 그 날의 기록을 다시 조회하고 날짜 시트를 닫는다. 오늘 이후 날짜는 고를 수 없어 무시한다. */
        fun selectDate(date: LocalDate) {
            val currentState = _uiState.value
            if (date.isAfter(currentState.today)) {
                return
            }
            _uiState.value =
                currentState.copy(
                    selectedDate = date,
                    visibleMonth = YearMonth.from(date),
                    isDatePickerVisible = false,
                )
            loadRecords()
            requestRecordDayMarkers()
        }

        fun selectToday() {
            selectDate(_uiState.value.today)
        }

        /** 날짜 선택 시트를 연다. 달력은 선택한 날짜가 속한 달을 보여주며, 그 달의 기록 점은 시트가 열릴 때 요청한다. */
        fun showDatePicker() {
            val currentState = _uiState.value
            _uiState.value =
                currentState.copy(
                    visibleMonth = YearMonth.from(currentState.selectedDate),
                    isDatePickerVisible = true,
                )
            requestRecordDayMarkers()
        }

        fun hideDatePicker() {
            _uiState.value = _uiState.value.copy(isDatePickerVisible = false)
        }

        /** 달력이 보이는 달을 [monthDelta]만큼 옮긴다. 오늘이 속한 달보다 미래로는 가지 않는다. */
        fun moveVisibleMonth(monthDelta: Int) {
            val currentState = _uiState.value
            val movedMonth = currentState.visibleMonth.plusMonths(monthDelta.toLong())
            if (movedMonth.isAfter(YearMonth.from(currentState.today))) {
                return
            }
            _uiState.value = currentState.copy(visibleMonth = movedMonth)
            requestRecordDayMarkers()
        }

        /** 유형 필터만 바꾼다. 이미 받은 목록을 화면에서 거르므로 서버를 다시 호출하지 않는다. */
        fun selectFilter(filter: RecordHistoryFilter) {
            _uiState.value = _uiState.value.copy(selectedFilter = filter)
        }

        private fun requestRecordDayMarkers() {
            val requestDates =
                _uiState.value.recordMarkerDates
                    .filterNot { date -> date in recordDayMarkers }
            if (requestDates.isEmpty()) {
                return
            }
            // 필요한 날짜가 이미 받는 중인 날짜의 부분 집합이면 진행 중인 조회를 그대로 둔다.
            if (markerJob?.isActive == true && requestingMarkerDates.containsAll(requestDates)) {
                return
            }
            markerJob?.cancel()
            requestingMarkerDates = requestDates.toSet()
            val owningGeneration = sessionManager.getSnapshot().generation
            markerJob =
                viewModelScope.launch {
                    val markers = getRecordDayMarkersUseCase(requestDates)
                    requestingMarkerDates = emptySet()
                    // 조회 중 계정이 바뀌었으면 이전 계정의 기록 유무를 반영하지 않는다.
                    if (owningGeneration != sessionManager.getSnapshot().generation) {
                        return@launch
                    }
                    recordDayMarkers += markers
                    _uiState.value = _uiState.value.copy(recordDayMarkers = recordDayMarkers.toMap())
                }
        }

        private fun clearRecordDayMarkers() {
            markerJob?.cancel()
            markerJob = null
            requestingMarkerDates = emptySet()
            recordDayMarkers.clear()
        }

        private fun createDefaultUiState(): RecordHistoryUiState {
            val today = LocalDate.now(clock)
            return RecordHistoryUiState(
                today = today,
                selectedDate = today,
                visibleMonth = YearMonth.from(today),
            )
        }
    }
