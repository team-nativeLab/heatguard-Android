package com.nativelap.heartguard.view.route.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.component.history.RecordHistoryDateRangePickerDialog
import com.nativelap.heartguard.view.screen.history.RecordHistoryScreen
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordHistoryViewModel
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import java.time.LocalDate

/** 기록 내역 화면에 들어올 때마다 기본 기간(최근 7일)으로 서버 기록을 조회한다.
 * 작업 위치는 홈 조회 값(team.workplace)을 쓰며, 기간 선택 다이얼로그 표시 여부는 ViewModel 상태로 관리해 회전 후에도 유지한다. */
@Composable
internal fun HeartGuardRecordHistoryRoute(
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onRecordClick: (String) -> Unit,
    onCreateRecordClick: () -> Unit,
    viewModel: RecordHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val siteStatus by homeViewModel.siteStatus.collectAsStateWithLifecycle()
    val today = uiState.today

    LaunchedEffect(viewModel) {
        viewModel.openHistory()
    }

    RecordHistoryScreen(
        uiState = uiState,
        workplace = siteStatus.workplace,
        onEvent = { event ->
            when (event) {
                RecordHistoryScreenEvent.BackClicked -> onBackClick()
                RecordHistoryScreenEvent.DateRangeClicked -> viewModel.showDateRangePicker()
                RecordHistoryScreenEvent.DateRangeDismissed -> viewModel.hideDateRangePicker()
                is RecordHistoryScreenEvent.DateRangeSelected -> viewModel.selectDateRange(
                    startDate = event.startDate,
                    endDate = event.endDate,
                )
                is RecordHistoryScreenEvent.FilterSelected -> viewModel.selectFilter(event.filter)
                is RecordHistoryScreenEvent.RecordClicked -> onRecordClick(event.recordId)
                RecordHistoryScreenEvent.RetryClicked -> viewModel.loadRecords()
                RecordHistoryScreenEvent.CreateRecordClicked -> onCreateRecordClick()
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
