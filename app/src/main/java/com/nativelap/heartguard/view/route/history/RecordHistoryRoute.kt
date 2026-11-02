package com.nativelap.heartguard.view.route.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.history.RecordHistoryDatePickerSheet
import com.nativelap.heartguard.view.screen.history.RecordHistoryScreen
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordHistoryViewModel
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 기록 내역 화면 Route다. 이 진입에서 처음 열 때만 선택한 날짜(기본 오늘)의 기록을 조회한다.
 * 날짜·날짜 선택 시트 상태는 ViewModel에 두어 회전 후에도 유지하고, 회전이나 상세 화면에서 돌아온 복귀에서는 다시 열지 않는다. */
@Composable
internal fun HeartGuardRecordHistoryRoute(
    onBackClick: () -> Unit,
    onRecordClick: (String) -> Unit,
    onCreateRecordClick: () -> Unit,
    onResumeDraftClick: (RecordType) -> Unit,
    recordDraftViewModel: RecordDraftViewModel,
    viewModel: RecordHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draftState by recordDraftViewModel.uiState.collectAsStateWithLifecycle()
    val pendingSubmissions by recordDraftViewModel.pendingSubmissions.collectAsStateWithLifecycle()
    val hasPendingCleanup by recordDraftViewModel.hasPendingCleanup.collectAsStateWithLifecycle()
    val hasPendingLoadFailed by recordDraftViewModel.hasPendingLoadFailed.collectAsStateWithLifecycle()
    // 이 진입에서 이미 열었는지 저장해, 회전 후 LaunchedEffect가 다시 돌아도 날짜와 시트 상태를 초기화하지 않는다.
    var hasOpenedHistory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        // 프로세스 복원으로 ViewModel이 새로 만들어졌으면 저장된 플래그와 달리 조회가 시작되지 않았으므로 다시 연다.
        if (!hasOpenedHistory || !viewModel.hasStartedHistory) {
            hasOpenedHistory = true
            viewModel.openHistory()
        }
        recordDraftViewModel.refreshPendingSubmissions()
    }

    val onEvent: (RecordHistoryScreenEvent) -> Unit = { event ->
        when (event) {
            RecordHistoryScreenEvent.BackClicked -> {
                onBackClick()
            }

            RecordHistoryScreenEvent.DatePickerClicked -> {
                viewModel.showDatePicker()
            }

            is RecordHistoryScreenEvent.DateSelected -> {
                viewModel.selectDate(event.date)
            }

            RecordHistoryScreenEvent.DatePickerDismissed -> {
                viewModel.hideDatePicker()
            }

            is RecordHistoryScreenEvent.MonthMoved -> {
                viewModel.moveVisibleMonth(event.monthDelta)
            }

            RecordHistoryScreenEvent.TodayClicked -> {
                viewModel.selectToday()
            }

            is RecordHistoryScreenEvent.FilterSelected -> {
                viewModel.selectFilter(event.filter)
            }

            is RecordHistoryScreenEvent.RecordClicked -> {
                onRecordClick(event.recordId)
            }

            RecordHistoryScreenEvent.TemporaryDraftClicked -> {
                draftState.selectedRecordType?.let { recordType ->
                    if (recordDraftViewModel.resumeTemporarilySavedDraft(recordType)) {
                        onResumeDraftClick(recordType)
                    }
                }
            }

            RecordHistoryScreenEvent.PendingSubmissionsRetryClicked -> {
                recordDraftViewModel.refreshPendingSubmissions()
            }

            RecordHistoryScreenEvent.RetryClicked -> {
                viewModel.loadRecords()
            }

            RecordHistoryScreenEvent.LoadMore -> {
                viewModel.loadMoreRecords()
            }

            RecordHistoryScreenEvent.CreateRecordClicked -> {
                onCreateRecordClick()
            }
        }
    }

    RecordHistoryScreen(
        uiState = uiState,
        pendingSubmissions = pendingSubmissions,
        hasPendingLoadFailed = hasPendingLoadFailed,
        hasPendingCleanup = hasPendingCleanup,
        temporaryDraftType = draftState.selectedRecordType.takeIf { draftState.isTemporarilySaved },
        temporaryDraftSavedAt = draftState.temporarilySavedAt,
        onEvent = onEvent,
    )

    if (uiState.isDatePickerVisible) {
        RecordHistoryDatePickerSheet(
            selectedDate = uiState.selectedDate,
            visibleMonth = uiState.visibleMonth,
            today = uiState.today,
            recordDayMarkers = uiState.recordDayMarkers,
            onMonthMoved = { monthDelta ->
                onEvent(RecordHistoryScreenEvent.MonthMoved(monthDelta))
            },
            onDateConfirmed = { date ->
                onEvent(RecordHistoryScreenEvent.DateSelected(date))
            },
            onDismissRequest = {
                onEvent(RecordHistoryScreenEvent.DatePickerDismissed)
            },
        )
    }
}
