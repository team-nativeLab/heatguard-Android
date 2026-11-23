package com.nativelap.heartguard.view.route.checklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import com.nativelap.heartguard.view.screen.checklist.ChecklistOverviewScreen
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel
import java.time.LocalTime

@Composable
internal fun ChecklistOverviewRoute(
    checklistViewModel: ChecklistViewModel,
    schedule: CheckSchedule?,
    siteLabel: String?,
    isScheduleLoading: Boolean,
    hasScheduleLoadError: Boolean,
    onBackClick: () -> Unit,
    onScheduleRetryClick: () -> Unit,
    onPeriodClick: (LocalTime) -> Unit,
    onNewDay: () -> Unit = {},
) {
    val checklistUiState by checklistViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(checklistViewModel, schedule) {
        checklistViewModel.setCheckSchedule(schedule)
    }

    LifecycleResumeEffect(checklistViewModel) {
        checklistViewModel.loadChecklist()
        onPauseOrDispose {}
    }

    ChecklistOverviewScreen(
        schedule = schedule,
        siteLabel = siteLabel,
        isLoading = isScheduleLoading,
        hasLoadError = hasScheduleLoadError,
        hasChecklistLoaded = checklistUiState.hasLoaded,
        hasChecklistLoadError = checklistUiState.hasLoadError,
        onBackClick = onBackClick,
        onRetryClick = onScheduleRetryClick,
        onChecklistRetryClick = checklistViewModel::retryLoad,
        onPeriodClick = onPeriodClick,
        onNewDay = onNewDay,
        onDateCheck = checklistViewModel::refreshIfDateChanged,
    )
}
