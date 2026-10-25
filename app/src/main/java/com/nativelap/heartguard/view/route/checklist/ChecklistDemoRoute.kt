package com.nativelap.heartguard.view.route.checklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.checklist.ChecklistOverviewScreen
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreen
import com.nativelap.heartguard.view.screen.checklist.ChecklistScreenEvent
import com.nativelap.heartguard.viewmodel.checklist.CHECKLIST_DEMO_DATE
import com.nativelap.heartguard.viewmodel.checklist.ChecklistDemoViewModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistItemUiModel
import java.time.LocalTime

@Composable
internal fun ChecklistDemoOverviewRoute(
    checklistDemoViewModel: ChecklistDemoViewModel,
    onBackClick: () -> Unit,
    onPeriodClick: (LocalTime) -> Unit,
) {
    val demoState by checklistDemoViewModel.uiState.collectAsStateWithLifecycle()

    ChecklistOverviewScreen(
        schedule = checklistDemoViewModel.checkSchedule(demoState),
        siteLabel = stringResource(R.string.checklist_demo_site_label),
        isLoading = false,
        hasLoadError = false,
        onBackClick = onBackClick,
        onRetryClick = {},
        onPeriodClick = onPeriodClick,
        fixedOverviewDate = CHECKLIST_DEMO_DATE,
    )
}

@Composable
internal fun ChecklistDemoPeriodRoute(
    checklistDemoViewModel: ChecklistDemoViewModel,
    time: String,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
) {
    val demoState by checklistDemoViewModel.uiState.collectAsStateWithLifecycle()
    val selectedTime = runCatching { LocalTime.parse(time) }.getOrNull()
    if (selectedTime == null) {
        LaunchedEffect(time) {
            onBackClick()
        }
        return
    }
    val selectedRound = demoState.rounds.firstOrNull { round -> round.time == selectedTime }
    if (selectedRound == null) {
        LaunchedEffect(selectedTime) {
            onBackClick()
        }
        return
    }
    val demoItemTitles =
        mapOf(
            R.string.checklist_demo_item_water to stringResource(R.string.checklist_demo_item_water),
            R.string.checklist_demo_item_shade to stringResource(R.string.checklist_demo_item_shade),
            R.string.checklist_demo_item_health to stringResource(R.string.checklist_demo_item_health),
            R.string.checklist_demo_item_work_pause to stringResource(R.string.checklist_demo_item_work_pause),
            R.string.checklist_demo_item_rest to stringResource(R.string.checklist_demo_item_rest),
            R.string.checklist_demo_item_first_aid to stringResource(R.string.checklist_demo_item_first_aid),
        )
    val checklistItems =
        selectedRound.items.map { item ->
            ChecklistItemUiModel(
                itemId = item.itemId,
                title = demoItemTitles.getValue(item.titleResourceId),
                isChecked = item.isChecked,
            )
        }
    val checklistUiState =
        checklistDemoViewModel.checklistUiState(
            time = selectedTime,
            items = checklistItems,
            state = demoState,
        )

    ChecklistScreen(
        uiState = checklistUiState,
        selectedTime = selectedTime,
        siteLabel = stringResource(R.string.checklist_demo_site_label),
        onEvent = { event ->
            when (event) {
                ChecklistScreenEvent.BackClicked -> {
                    onBackClick()
                }

                is ChecklistScreenEvent.ItemToggled -> {
                    checklistDemoViewModel.setItemChecked(
                        time = selectedTime,
                        itemId = event.itemId,
                        isChecked = event.isChecked,
                    )
                }

                ChecklistScreenEvent.RetryClicked -> {
                    Unit
                }

                ChecklistScreenEvent.CompleteClicked -> {
                    if (checklistUiState.isComplete) {
                        checklistDemoViewModel.completeRound(selectedTime)
                        onCompleteClick()
                    }
                }
            }
        },
    )
}
