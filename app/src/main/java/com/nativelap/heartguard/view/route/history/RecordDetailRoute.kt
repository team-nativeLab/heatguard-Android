package com.nativelap.heartguard.view.route.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.screen.history.RecordDetailScreen
import com.nativelap.heartguard.viewmodel.history.RecordDetailScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordDetailViewModel

/** [recordId] 기록 상세를 조회해 보여준다. */
@Composable
internal fun HeartGuardRecordDetailRoute(
    recordId: String,
    onBackClick: () -> Unit,
    viewModel: RecordDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, recordId) {
        viewModel.loadRecordDetail(recordId)
    }

    RecordDetailScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                RecordDetailScreenEvent.BackClicked -> onBackClick()
                RecordDetailScreenEvent.RetryClicked -> viewModel.loadRecordDetail(recordId)
            }
        },
    )
}
