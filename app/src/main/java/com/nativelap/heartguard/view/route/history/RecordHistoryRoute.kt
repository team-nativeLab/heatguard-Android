package com.nativelap.heartguard.view.route.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.component.history.RecordHistoryDateRangePickerDialog
import com.nativelap.heartguard.view.screen.history.RecordHistoryScreen
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordHistoryViewModel
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import java.time.LocalDate

/** 기록 내역 화면에 들어올 때마다 기본 기간(최근 7일)으로 서버 기록을 조회한다.
 * 기간 선택 다이얼로그 표시 여부는 ViewModel 상태로 관리해 회전 후에도 유지한다. */
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
    val today = uiState.today

    LaunchedEffect(viewModel) {
        viewModel.openHistory()
        recordDraftViewModel.refreshPendingSubmissions()
    }

    RecordHistoryScreen(
        uiState = uiState,
        pendingSubmissions = pendingSubmissions,
        hasPendingLoadFailed = hasPendingLoadFailed,
        hasPendingCleanup = hasPendingCleanup,
        temporaryDraftType = draftState.selectedRecordType.takeIf { draftState.isTemporarilySaved },
        temporaryDraftSavedAt = draftState.temporarilySavedAt,
        onEvent = { event ->
            when (event) {
                RecordHistoryScreenEvent.BackClicked -> {
                    onBackClick()
                }

                RecordHistoryScreenEvent.DateRangeClicked -> {
                    viewModel.showDateRangePicker()
                }

                RecordHistoryScreenEvent.DateRangeDismissed -> {
                    viewModel.hideDateRangePicker()
                }

                is RecordHistoryScreenEvent.DateRangeSelected -> {
                    viewModel.selectDateRange(
                        startDate = event.startDate,
                        endDate = event.endDate,
                    )
                }

                is RecordHistoryScreenEvent.FilterSelected -> {
                    viewModel.selectFilter(event.filter)
                }

                is RecordHistoryScreenEvent.RecordClicked -> {
                    onRecordClick(event.recordId)
                }

                RecordHistoryScreenEvent.TemporaryDraftClicked -> {
                    draftState.selectedRecordType?.let { recordType ->
                        recordDraftViewModel.resumeTemporarilySavedDraft()
                        onResumeDraftClick(recordType)
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
        },
    )

    if (uiState.isDateRangePickerVisible) {
        RecordHistoryDateRangePickerDialog(
            initialStartDate = uiState.startDate,
            initialEndDate = uiState.endDate,
            today = today,
            onConfirm = { startDate: LocalDate, endDate: LocalDate ->
                viewModel.selectDateRange(
                    startDate = startDate,
                    endDate = endDate,
                )
            },
            onDismiss = viewModel::hideDateRangePicker,
        )
    }
}
