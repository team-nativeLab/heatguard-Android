package com.nativelap.heartguard.view.route.checklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreen
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreenEvent
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel
import kotlinx.coroutines.delay

@Composable
internal fun ChecklistRoute(
    checklistViewModel: ChecklistViewModel,
    checkSchedule: CheckSchedule?,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
) {
    val uiState by checklistViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(checklistViewModel, checkSchedule) {
        checklistViewModel.setCheckSchedule(checkSchedule)
    }

    LifecycleResumeEffect(checklistViewModel) {
        checklistViewModel.loadChecklist()
        onPauseOrDispose {}
    }

    LaunchedEffect(checklistViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(CHECKLIST_DATE_REFRESH_INTERVAL_MILLIS)
                checklistViewModel.refreshIfDateChanged()
            }
        }
    }

    ChecklistScreen(
        uiState = uiState,
        onEvent = { event ->
            when (event) {
                ChecklistScreenEvent.BackClicked -> {
                    onBackClick()
                }

                is ChecklistScreenEvent.ItemToggled -> {
                    checklistViewModel.setChecklistItemChecked(
                        itemId = event.itemId,
                        isChecked = event.isChecked,
                    )
                }

                ChecklistScreenEvent.RetryClicked -> {
                    checklistViewModel.retryLoad()
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

private const val CHECKLIST_DATE_REFRESH_INTERVAL_MILLIS = 60_000L
