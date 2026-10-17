package com.nativelap.heartguard.view.route.checklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreen
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreenEvent
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel

@Composable
internal fun ChecklistRoute(
    checklistViewModel: ChecklistViewModel,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
) {
    val uiState by checklistViewModel.uiState.collectAsStateWithLifecycle()

    ChecklistScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                ChecklistScreenEvent.BackClicked -> onBackClick()
                is ChecklistScreenEvent.ItemToggled -> {
                    checklistViewModel.setChecklistItemChecked(
                        titleResId = event.titleResId,
                        isChecked = event.isChecked,
                    )
                }

                ChecklistScreenEvent.CompleteClicked -> {
                    if (uiState.isComplete) {
                        onCompleteClick()
                    }
                }
            }
        },
    )
}
