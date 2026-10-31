package com.nativelap.heartguard.view.route.checklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.view.screen.checklist.ChecklistOverviewScreen
import com.nativelap.heartguard.view.screen.checklist.ChecklistRoundDetailScreen
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundsViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
internal fun ChecklistRoundsRoute(
    viewModel: ChecklistRoundsViewModel,
    siteLabel: String?,
    onBackClick: () -> Unit,
    onRoundClick: (LocalDate, String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadRounds()
            while (true) {
                delay(OVERVIEW_REFRESH_INTERVAL_MILLIS)
                viewModel.loadRounds()
            }
        }
    }

    ChecklistOverviewScreen(
        schedule = null,
        siteLabel = siteLabel,
        isLoading = state.isLoadingRounds,
        hasLoadError = state.hasRoundsLoadError,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retryRounds,
        onPeriodClick = {},
        roundsUiState = state,
        onRoundClick = onRoundClick,
    )
}

@Composable
internal fun ChecklistRoundDetailRoute(
    viewModel: ChecklistRoundsViewModel,
    date: LocalDate,
    roundId: String,
    siteLabel: String?,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner, date, roundId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadRound(roundId, date)
            while (true) {
                delay(viewModel.nextRoundRefreshDelayMillis(roundId, date))
                viewModel.loadRound(roundId, date)
            }
        }
    }

    ChecklistRoundDetailScreen(
        uiState = state,
        date = date,
        roundId = roundId,
        siteLabel = siteLabel,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retryRound,
        onRetryRoundsClick = { viewModel.loadRounds(date) },
        onItemCheckedChange = { itemId, checked ->
            viewModel.setRoundItemChecked(roundId, date, itemId, checked)
        },
    )
}

private const val OVERVIEW_REFRESH_INTERVAL_MILLIS = 60_000L
