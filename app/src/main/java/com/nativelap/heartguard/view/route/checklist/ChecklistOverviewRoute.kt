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
import com.nativelap.heartguard.view.screen.checklist.ChecklistOverviewScreen
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel
import kotlinx.coroutines.delay
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(checklistViewModel, schedule) {
        checklistViewModel.setCheckSchedule(schedule)
    }

    LifecycleResumeEffect(checklistViewModel) {
        checklistViewModel.loadChecklist()
        onPauseOrDispose {}
    }

    LaunchedEffect(checklistViewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(CHECKLIST_OVERVIEW_DATE_REFRESH_INTERVAL_MILLIS)
                checklistViewModel.refreshIfDateChanged()
            }
        }
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
        onNewDay = {
            checklistViewModel.refreshIfDateChanged()
            onNewDay()
        },
    )
}

private const val CHECKLIST_OVERVIEW_DATE_REFRESH_INTERVAL_MILLIS = 60_000L
