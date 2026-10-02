package com.nativelap.heartguard.view.route.notification

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(viewModel) {
        viewModel.openNotifications()
        viewModel.viewEffects.collect { effect ->
            when (effect) {
                TeamNotificationViewEffect.ReadFailed -> {
                    snackbarHostState.showSnackbar(readErrorMessage)
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
                TeamNotificationScreenEvent.RetryInitialLoad -> viewModel.retryInitialLoad()
                TeamNotificationScreenEvent.LoadMore -> viewModel.loadNextPage()
                TeamNotificationScreenEvent.RetryLoadMore -> viewModel.retryNextPage()
            }
        },
    )
}
