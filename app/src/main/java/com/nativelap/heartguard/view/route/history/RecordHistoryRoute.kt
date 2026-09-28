package com.nativelap.heartguard.view.route.history

import androidx.compose.runtime.Composable
import com.nativelap.heartguard.view.screen.history.RecordHistoryScreen
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent

/** 작업자 기록 목록 API가 없는 상태를 표시하고 뒤로 가기만 Navigation에 연결한다. */
@Composable
internal fun HeartGuardRecordHistoryRoute(
    onBackClick: () -> Unit,
) {
    RecordHistoryScreen(
        onEvent = { event ->
            when (event) {
                RecordHistoryScreenEvent.BackClicked -> onBackClick()
            }
        },
    )
}
