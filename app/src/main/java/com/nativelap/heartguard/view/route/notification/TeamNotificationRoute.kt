package com.nativelap.heartguard.view.route.notification

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.view.screen.notification.TeamNotificationScreen
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationScreenEvent
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationViewEffect
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationViewModel
import kotlinx.coroutines.flow.collect

@Composable
internal fun HeartGuardTeamNotificationsRoute(
    onBackClick: () -> Unit,
    viewModel: TeamNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val readErrorMessage = stringResource(R.string.notification_read_error)
    val readAllErrorMessage = stringResource(R.string.notification_read_all_error)
    val refreshErrorMessage = stringResource(R.string.notification_refresh_error)
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasResumed by remember(viewModel) { mutableStateOf(false) }

    LifecycleResumeEffect(viewModel) {
        if (hasResumed) {
            viewModel.refreshOnResume()
        } else {
            hasResumed = true
            viewModel.openNotifications()
        }
        onPauseOrDispose {
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.viewEffects.collect { effect ->
                when (effect) {
                    TeamNotificationViewEffect.ReadFailed -> {
                        snackbarHostState.showSnackbar(readErrorMessage)
                    }
                    TeamNotificationViewEffect.ReadAllFailed -> {
                        snackbarHostState.showSnackbar(readAllErrorMessage)
                    }
                    TeamNotificationViewEffect.RefreshFailed -> {
                        snackbarHostState.showSnackbar(refreshErrorMessage)
                    }
                }
            }
        }
    }

    TeamNotificationScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = { event ->
            when (event) {
                TeamNotificationScreenEvent.BackClicked -> onBackClick()
                is TeamNotificationScreenEvent.CategorySelected -> viewModel.selectCategory(event.category)
                is TeamNotificationScreenEvent.NotificationClicked -> viewModel.markRead(event.notificationId)
                TeamNotificationScreenEvent.MarkAllReadClicked -> viewModel.markAllRead()
                TeamNotificationScreenEvent.RetryInitialLoad -> viewModel.retryInitialLoad()
                TeamNotificationScreenEvent.RetryRefresh -> viewModel.retryRefresh()
                TeamNotificationScreenEvent.LoadMore -> viewModel.loadNextPage()
                TeamNotificationScreenEvent.RetryLoadMore -> viewModel.retryNextPage()
            }
        },
    )
}
