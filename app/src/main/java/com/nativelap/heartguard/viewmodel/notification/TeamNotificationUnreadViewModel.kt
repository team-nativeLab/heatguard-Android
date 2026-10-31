package com.nativelap.heartguard.viewmodel.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TeamNotificationUnreadViewModel
    @Inject
    constructor(
        private val getTeamNotificationsUseCase: GetTeamNotificationsUseCase,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(TeamNotificationUnreadUiState())
        val uiState = mutableUiState.asStateFlow()

        private var refreshJob: Job? = null
        private var requestGeneration = 0L

        init {
            clearStateWhenSessionEnds(sessionManager) {
                requestGeneration += 1L
                refreshJob?.cancel()
                refreshJob = null
                mutableUiState.value = TeamNotificationUnreadUiState()
            }
        }

        fun refresh() {
            if (sessionManager.sessionState.value != SessionState.Authenticated) {
                return
            }

            refreshJob?.cancel()
            val requestSessionGeneration = sessionManager.getSnapshot().generation
            val currentRequestGeneration = ++requestGeneration
            refreshJob =
                viewModelScope.launch {
                    try {
                        when (
                            val pageResult =
                                getTeamNotificationsUseCase(
                                    category = NotificationCategory.ALL,
                                    cursor = null,
                                    limit = PAGE_SIZE,
                                )
                        ) {
                            is ApiResult.Success -> {
                                if (isCurrentRequest(currentRequestGeneration, requestSessionGeneration)) {
                                    mutableUiState.value =
                                        TeamNotificationUnreadUiState(
                                            unreadCount = pageResult.value.unreadCount,
                                        )
                                }
                            }

                            is ApiResult.Failure -> Unit
                        }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        // Keep the last server-confirmed count when a refresh fails.
                    }
                }
        }

        private suspend fun isCurrentRequest(
            expectedRequestGeneration: Long,
            expectedSessionGeneration: Long,
        ): Boolean =
            currentCoroutineContext().isActive &&
                expectedRequestGeneration == requestGeneration &&
                expectedSessionGeneration == sessionManager.getSnapshot().generation &&
                sessionManager.sessionState.value == SessionState.Authenticated

        private companion object {
            const val PAGE_SIZE = 20
        }
    }
