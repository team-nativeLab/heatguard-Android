package com.nativelap.heartguard.viewmodel.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkTeamNotificationReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TeamNotificationViewModel @Inject constructor(
    private val getTeamNotificationsUseCase: GetTeamNotificationsUseCase,
    private val markTeamNotificationReadUseCase: MarkTeamNotificationReadUseCase,
    sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeamNotificationUiState())
    val uiState: StateFlow<TeamNotificationUiState> = _uiState.asStateFlow()

    private val _viewEffects = MutableSharedFlow<TeamNotificationViewEffect>(
        extraBufferCapacity = VIEW_EFFECT_BUFFER_CAPACITY,
    )
    val viewEffects: SharedFlow<TeamNotificationViewEffect> = _viewEffects.asSharedFlow()

    private var listRequestJob: Job? = null
    private val readRequestJobs = mutableMapOf<String, Job>()
    private var listRequestGeneration = 0

    init {
        clearStateWhenSessionEnds(sessionManager) {
            listRequestGeneration += 1
            listRequestJob?.cancel()
            listRequestJob = null
            readRequestJobs.values.forEach(Job::cancel)
            readRequestJobs.clear()
            _uiState.value = TeamNotificationUiState()
        }
    }

    fun openNotifications() {
        loadFirstPage(NotificationCategory.ALL)
    }

    fun selectCategory(category: NotificationCategory) {
        if (category == NotificationCategory.UNKNOWN || category == _uiState.value.selectedCategory) return
        loadFirstPage(category)
    }

    fun retryInitialLoad() {
        loadFirstPage(_uiState.value.selectedCategory)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val requestCursor = currentState.nextCursor ?: return
        if (!currentState.hasMore || currentState.isLoadingMore || currentState.hasLoadMoreError) return
        requestNextPage(currentState, requestCursor)
    }

    fun retryNextPage() {
        val currentState = _uiState.value
        if (!currentState.hasLoadMoreError) return
        _uiState.value = currentState.copy(hasLoadMoreError = false)
        loadNextPage()
    }

    fun markRead(notificationId: String) {
        val currentState = _uiState.value
        val notification = currentState.notifications.firstOrNull { item ->
            item.notificationId == notificationId
        } ?: return
        if (notification.isRead || notificationId in currentState.markingReadIds) return

        _uiState.value = currentState.copy(markingReadIds = currentState.markingReadIds + notificationId)
        readRequestJobs[notificationId] = viewModelScope.launch {
            when (val readResult = markTeamNotificationReadUseCase(notificationId)) {
                is ApiResult.Success -> {
                    val current = _uiState.value
                    val previousItem = current.notifications.firstOrNull { item ->
                        item.notificationId == notificationId
                    }
                    val shouldUpdateCounts = previousItem?.isRead == false && readResult.value.isRead
                    _uiState.value = current.copy(
                        notifications = current.notifications.map { item ->
                            if (item.notificationId == notificationId) item.copy(isRead = readResult.value.isRead) else item
                        },
                        unreadCount = if (shouldUpdateCounts) (current.unreadCount - 1).coerceAtLeast(0) else current.unreadCount,
                        filteredUnreadCount = if (shouldUpdateCounts && matchesSelectedCategory(previousItem, current.selectedCategory)) {
                            (current.filteredUnreadCount - 1).coerceAtLeast(0)
                        } else {
                            current.filteredUnreadCount
                        },
                        markingReadIds = current.markingReadIds - notificationId,
                    )
                }

                is ApiResult.Failure -> {
                    _uiState.value = _uiState.value.copy(markingReadIds = _uiState.value.markingReadIds - notificationId)
                    _viewEffects.emit(TeamNotificationViewEffect.ReadFailed)
                }
            }
            readRequestJobs.remove(notificationId)
        }
    }

    private fun loadFirstPage(category: NotificationCategory) {
        val requestGeneration = ++listRequestGeneration
        listRequestJob?.cancel()
        _uiState.value = TeamNotificationUiState(
            selectedCategory = category,
            isInitialLoading = true,
        )
        listRequestJob = viewModelScope.launch {
            when (val pageResult = getTeamNotificationsUseCase(category, cursor = null, limit = PAGE_SIZE)) {
                is ApiResult.Success -> {
                    if (requestGeneration != listRequestGeneration) return@launch
                    _uiState.value = pageResult.value.toUiState(category)
                }

                is ApiResult.Failure -> {
                    if (requestGeneration != listRequestGeneration) return@launch
                    _uiState.value = TeamNotificationUiState(
                        selectedCategory = category,
                        hasLoaded = true,
                        hasInitialError = true,
                    )
                }
            }
        }
    }

    private fun requestNextPage(
        currentState: TeamNotificationUiState,
        cursor: String,
    ) {
        val requestGeneration = listRequestGeneration
        _uiState.value = currentState.copy(isLoadingMore = true, hasLoadMoreError = false)
        listRequestJob = viewModelScope.launch {
            when (
                val pageResult = getTeamNotificationsUseCase(
                    category = currentState.selectedCategory,
                    cursor = cursor,
                    limit = PAGE_SIZE,
                )
            ) {
                is ApiResult.Success -> {
                    if (requestGeneration != listRequestGeneration) return@launch
                    val latestState = _uiState.value
                    val mergedNotifications = (latestState.notifications + pageResult.value.items)
                        .distinctBy(TeamNotification::notificationId)
                        .sortedWith(compareByDescending<TeamNotification> { item -> item.createdAt?.toInstant() ?: Instant.MIN })
                    val receivedCursor = pageResult.value.nextCursor
                    _uiState.value = latestState.copy(
                        notifications = mergedNotifications,
                        nextCursor = receivedCursor,
                        hasMore = pageResult.value.hasMore && receivedCursor != null && receivedCursor != cursor,
                        isLoadingMore = false,
                        hasLoadMoreError = false,
                        unreadCount = pageResult.value.unreadCount,
                        filteredUnreadCount = pageResult.value.filteredUnreadCount,
                    )
                }

                is ApiResult.Failure -> {
                    if (requestGeneration != listRequestGeneration) return@launch
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, hasLoadMoreError = true)
                }
            }
        }
    }

    private fun com.nativelap.heartguard.domain.notification.model.TeamNotificationPage.toUiState(
        category: NotificationCategory,
    ): TeamNotificationUiState {
        val receivedCursor = nextCursor
        return TeamNotificationUiState(
            selectedCategory = category,
            notifications = items.distinctBy(TeamNotification::notificationId)
                .sortedWith(compareByDescending<TeamNotification> { item -> item.createdAt?.toInstant() ?: Instant.MIN }),
            hasLoaded = true,
            nextCursor = receivedCursor,
            hasMore = hasMore && receivedCursor != null,
            unreadCount = unreadCount,
            filteredUnreadCount = filteredUnreadCount,
        )
    }

    private fun matchesSelectedCategory(
        notification: TeamNotification?,
        selectedCategory: NotificationCategory,
    ): Boolean = selectedCategory == NotificationCategory.ALL || notification?.category == selectedCategory

    private companion object {
        const val PAGE_SIZE = 20
        const val VIEW_EFFECT_BUFFER_CAPACITY = 1
    }
}
