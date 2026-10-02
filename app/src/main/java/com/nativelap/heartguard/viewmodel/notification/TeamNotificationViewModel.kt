package com.nativelap.heartguard.viewmodel.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkAllTeamNotificationsReadUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkTeamNotificationReadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TeamNotificationViewModel @Inject constructor(
    private val getTeamNotificationsUseCase: GetTeamNotificationsUseCase,
    private val markTeamNotificationReadUseCase: MarkTeamNotificationReadUseCase,
    private val markAllTeamNotificationsReadUseCase: MarkAllTeamNotificationsReadUseCase,
    sessionManager: SessionManager,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeamNotificationUiState())
    val uiState: StateFlow<TeamNotificationUiState> = _uiState.asStateFlow()

    private val _viewEffects = MutableSharedFlow<TeamNotificationViewEffect>(
        extraBufferCapacity = VIEW_EFFECT_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val viewEffects: SharedFlow<TeamNotificationViewEffect> = _viewEffects.asSharedFlow()

    private var listRequestJob: Job? = null
    private val readRequestJobs = mutableMapOf<String, Job>()
    private var readAllRequestJob: Job? = null
    private var listRequestGeneration = 0
    private var readRequestGeneration = 0
    private var sessionGeneration = 0
    private var pendingFirstPageRequest: FirstPageRequest? = null
    private var lastFirstPageStartMillis: Long? = null

    // 현재 분류에서 이미 받은 커서다. 첫 페이지를 새로 받을 때와 세션이 바뀔 때 비운다.
    private val loadedCursors = mutableSetOf<String>()

    init {
        clearStateWhenSessionEnds(sessionManager) {
            pendingFirstPageRequest = null
            lastFirstPageStartMillis = null
            loadedCursors.clear()
            sessionGeneration += 1
            listRequestGeneration += 1
            readRequestGeneration += 1
            listRequestJob?.cancel()
            listRequestJob = null
            readRequestJobs.values.forEach(Job::cancel)
            readRequestJobs.clear()
            readAllRequestJob?.cancel()
            readAllRequestJob = null
            _uiState.value = TeamNotificationUiState()
        }
    }

    /** 진행 중인 일괄 처리는 유지하고 현재 분류를 다시 조회한다. */
    fun openNotifications() {
        val currentState = _uiState.value
        if (hasReadAllRequest() || listRequestJob?.isActive == true || pendingFirstPageRequest != null) {
            return
        }
        loadFirstPage(
            category = currentState.selectedCategory,
            preserveContent = currentState.hasLoaded && !currentState.hasInitialError,
        )
    }

    /** 앱 복귀 시 진행 중인 요청을 유지하고 최소 간격 뒤 목록을 갱신한다. */
    fun refreshOnResume() {
        if (listRequestJob?.isActive == true ||
            hasReadAllRequest() || pendingFirstPageRequest != null
        ) {
            return
        }
        val previousStartMillis = lastFirstPageStartMillis
        if (previousStartMillis != null && clock.millis() - previousStartMillis < RESUME_REFRESH_INTERVAL_MILLIS) {
            return
        }
        if (readRequestJobs.isNotEmpty()) {
            val currentState = _uiState.value
            loadFirstPage(
                category = currentState.selectedCategory,
                preserveContent = currentState.hasLoaded && !currentState.hasInitialError,
            )
        } else {
            openNotifications()
        }
    }

    fun selectCategory(category: NotificationCategory) {
        if (category == NotificationCategory.UNKNOWN ||
            category == (pendingFirstPageRequest?.category ?: _uiState.value.selectedCategory) || hasReadAllRequest()
        ) {
            return
        }
        loadFirstPage(category)
    }

    fun retryInitialLoad() {
        if (hasReadAllRequest() || _uiState.value.isRefreshing) {
            return
        }
        loadFirstPage(_uiState.value.selectedCategory)
    }

    fun retryRefresh() {
        val currentState = _uiState.value
        if (!currentState.hasRefreshError || hasReadAllRequest() || currentState.isRefreshing) {
            return
        }
        loadFirstPage(currentState.selectedCategory, preserveContent = true)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val requestCursor = currentState.nextCursor ?: return
        if (readRequestJobs.isNotEmpty() || pendingFirstPageRequest != null ||
            isReadMutationBlocked(currentState) || !currentState.hasMore ||
            currentState.isLoadingMore || currentState.hasLoadMoreError
        ) {
            return
        }
        requestNextPage(currentState, requestCursor)
    }

    fun retryNextPage() {
        val currentState = _uiState.value
        if (readRequestJobs.isNotEmpty() || pendingFirstPageRequest != null ||
            !currentState.hasLoadMoreError || isReadMutationBlocked(currentState)
        ) {
            return
        }
        _uiState.value = currentState.copy(hasLoadMoreError = false)
        loadNextPage()
    }

    /** 아직 읽지 않은 한 건을 처리하고 서버 결과를 목록에 반영한다. */
    fun markRead(notificationId: String) {
        val currentState = _uiState.value
        if (isReadMutationBlocked(currentState)) {
            return
        }
        val notification = currentState.notifications.firstOrNull { candidate ->
            candidate.notificationId == notificationId
        } ?: return
        if (notification.isRead || readRequestJobs.containsKey(notificationId)) {
            return
        }
        val requestSession = sessionGeneration
        val requestGeneration = readRequestGeneration
        if (currentState.isLoadingMore) {
            listRequestGeneration += 1
            listRequestJob?.cancel()
            listRequestJob = null
            pendingFirstPageRequest = FirstPageRequest(currentState.selectedCategory, preserveContent = true)
        }
        _uiState.value = currentState.copy(
            isLoadingMore = false,
            markingReadIds = currentState.markingReadIds + notificationId,
        )
        val requestJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val runningJob = currentCoroutineContext()[Job]
            try {
                val readResult = markTeamNotificationReadUseCase(notificationId)
                if (!isActive || requestSession != sessionGeneration ||
                    requestGeneration != readRequestGeneration || readRequestJobs[notificationId] !== runningJob
                ) {
                    return@launch
                }
                when (readResult) {
                    is ApiResult.Success -> {
                        val latestState = _uiState.value
                        val previousNotification = latestState.notifications.firstOrNull { candidate ->
                            candidate.notificationId == notificationId
                        }
                        val shouldUpdateCounts = previousNotification?.isRead == false && readResult.value.isRead
                        _uiState.value = latestState.copy(
                            notifications = latestState.notifications.map { candidate ->
                                if (candidate.notificationId == notificationId) {
                                    candidate.copy(isRead = readResult.value.isRead)
                                } else {
                                    candidate
                                }
                            },
                            unreadCount = if (shouldUpdateCounts) {
                                (latestState.unreadCount - 1).coerceAtLeast(0)
                            } else {
                                latestState.unreadCount
                            },
                            filteredUnreadCount = if (shouldUpdateCounts &&
                                matchesSelectedCategory(previousNotification, latestState.selectedCategory)
                            ) {
                                (latestState.filteredUnreadCount - 1).coerceAtLeast(0)
                            } else {
                                latestState.filteredUnreadCount
                            },
                            markingReadIds = latestState.markingReadIds - notificationId,
                        )
                    }
                    is ApiResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            markingReadIds = _uiState.value.markingReadIds - notificationId,
                        )
                        _viewEffects.tryEmit(TeamNotificationViewEffect.ReadFailed)
                    }
                }
            } finally {
                if (requestSession == sessionGeneration && requestGeneration == readRequestGeneration &&
                    readRequestJobs[notificationId] === runningJob
                ) {
                    readRequestJobs.remove(notificationId)
                    if (readRequestJobs.isEmpty()) {
                        val pendingRequest = pendingFirstPageRequest
                        pendingFirstPageRequest = null
                        if (pendingRequest != null) {
                            loadFirstPage(pendingRequest.category, pendingRequest.preserveContent)
                        }
                    }
                }
            }
        }
        readRequestJobs[notificationId] = requestJob
        requestJob.start()
    }

    /** 전체 읽음 성공을 반영한 뒤 목록을 보존하며 최신 첫 페이지를 조회한다. */
    fun markAllRead() {
        val currentState = _uiState.value
        if (!currentState.hasLoaded || currentState.hasInitialError || currentState.unreadCount == 0 ||
            isReadMutationBlocked(currentState)
        ) {
            return
        }
        pendingFirstPageRequest = null
        readRequestGeneration += 1
        readRequestJobs.values.forEach(Job::cancel)
        readRequestJobs.clear()
        listRequestGeneration += 1
        listRequestJob?.cancel()
        listRequestJob = null
        val requestSession = sessionGeneration
        _uiState.value = currentState.copy(
            markingReadIds = emptySet(),
            isLoadingMore = false,
            isMarkingAllRead = true,
        )
        val requestJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val runningJob = currentCoroutineContext()[Job]
            try {
                val readAllResult = markAllTeamNotificationsReadUseCase()
                if (!isActive || requestSession != sessionGeneration || readAllRequestJob !== runningJob) {
                    return@launch
                }
                when (readAllResult) {
                    is ApiResult.Success -> {
                        val latestState = _uiState.value
                        val noUnreadNotifications = readAllResult.value.unreadCount == 0
                        _uiState.value = latestState.copy(
                            notifications = if (noUnreadNotifications) {
                                latestState.notifications.map { notification -> notification.copy(isRead = true) }
                            } else {
                                latestState.notifications
                            },
                            unreadCount = readAllResult.value.unreadCount,
                            filteredUnreadCount = if (noUnreadNotifications) {
                                0
                            } else {
                                latestState.filteredUnreadCount
                            },
                            isMarkingAllRead = false,
                        )
                        loadFirstPage(latestState.selectedCategory, preserveContent = true)
                    }
                    is ApiResult.Failure -> {
                        _uiState.value = _uiState.value.copy(isMarkingAllRead = false)
                        _viewEffects.tryEmit(TeamNotificationViewEffect.ReadAllFailed)
                    }
                }
            } finally {
                if (readAllRequestJob === runningJob) {
                    readAllRequestJob = null
                }
            }
        }
        readAllRequestJob = requestJob
        requestJob.start()
    }

    private fun loadFirstPage(
        category: NotificationCategory,
        preserveContent: Boolean = false,
    ) {
        if (readRequestJobs.isNotEmpty()) {
            pendingFirstPageRequest = FirstPageRequest(category, preserveContent)
            return
        }
        lastFirstPageStartMillis = clock.millis()
        loadedCursors.clear()
        val requestGeneration = ++listRequestGeneration
        val requestSession = sessionGeneration
        listRequestJob?.cancel()
        _uiState.value = if (preserveContent) {
            _uiState.value.copy(
                isRefreshing = true,
                hasRefreshError = false,
                nextCursor = null,
                hasMore = false,
                isLoadingMore = false,
                hasLoadMoreError = false,
            )
        } else {
            TeamNotificationUiState(selectedCategory = category, isInitialLoading = true)
        }
        val requestJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val runningJob = currentCoroutineContext()[Job]
            try {
                val pageResult = getTeamNotificationsUseCase(category, cursor = null, limit = PAGE_SIZE)
                if (!isActive || requestSession != sessionGeneration ||
                    requestGeneration != listRequestGeneration || listRequestJob !== runningJob
                ) {
                    return@launch
                }
                when (pageResult) {
                    is ApiResult.Success -> {
                        _uiState.value = pageResult.value.toUiState(category)
                    }
                    is ApiResult.Failure -> {
                        if (preserveContent) {
                            _uiState.value = _uiState.value.copy(isRefreshing = false, hasRefreshError = true)
                            _viewEffects.tryEmit(TeamNotificationViewEffect.RefreshFailed)
                        } else {
                            _uiState.value = TeamNotificationUiState(
                                selectedCategory = category,
                                hasLoaded = true,
                                hasInitialError = true,
                            )
                        }
                    }
                }
            } finally {
                if (listRequestJob === runningJob) {
                    listRequestJob = null
                }
            }
        }
        listRequestJob = requestJob
        requestJob.start()
    }

    private fun requestNextPage(
        currentState: TeamNotificationUiState,
        cursor: String,
    ) {
        val requestGeneration = listRequestGeneration
        val requestSession = sessionGeneration
        _uiState.value = currentState.copy(isLoadingMore = true, hasLoadMoreError = false)
        val requestJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val runningJob = currentCoroutineContext()[Job]
            try {
                val pageResult = getTeamNotificationsUseCase(
                    category = currentState.selectedCategory,
                    cursor = cursor,
                    limit = PAGE_SIZE,
                )
                if (!isActive || requestSession != sessionGeneration ||
                    requestGeneration != listRequestGeneration || listRequestJob !== runningJob
                ) {
                    return@launch
                }
                when (pageResult) {
                    is ApiResult.Success -> {
                        val latestState = _uiState.value
                        val mergedNotifications = (latestState.notifications + pageResult.value.items)
                            .distinctBy(TeamNotification::notificationId)
                            .sortedWith(compareByDescending { notification -> notification.createdAt?.toInstant() ?: Instant.MIN })
                        val receivedCursor = pageResult.value.nextCursor
                        loadedCursors += cursor
                        // 이미 받은 커서가 다시 오면 같은 페이지를 반복하지 않고 목록을 끝내며 실패를 알린다.
                        val isRepeatedCursor = receivedCursor != null && receivedCursor in loadedCursors
                        _uiState.value = latestState.copy(
                            notifications = mergedNotifications,
                            nextCursor = receivedCursor.takeUnless { isRepeatedCursor },
                            hasMore = pageResult.value.hasMore && receivedCursor != null && !isRepeatedCursor,
                            isLoadingMore = false,
                            hasLoadMoreError = isRepeatedCursor,
                            unreadCount = pageResult.value.unreadCount,
                            filteredUnreadCount = pageResult.value.filteredUnreadCount,
                        )
                    }
                    is ApiResult.Failure -> {
                        _uiState.value = _uiState.value.copy(isLoadingMore = false, hasLoadMoreError = true)
                    }
                }
            } finally {
                if (listRequestJob === runningJob) {
                    listRequestJob = null
                }
            }
        }
        listRequestJob = requestJob
        requestJob.start()
    }

    private fun hasReadAllRequest(): Boolean =
        readAllRequestJob?.isActive == true || _uiState.value.isMarkingAllRead

    private fun isReadMutationBlocked(state: TeamNotificationUiState): Boolean =
        hasReadAllRequest() || state.isRefreshing || state.hasRefreshError

    private fun TeamNotificationPage.toUiState(category: NotificationCategory): TeamNotificationUiState {
        val receivedCursor = nextCursor
        return TeamNotificationUiState(
            selectedCategory = category,
            notifications = items.distinctBy(TeamNotification::notificationId)
                .sortedWith(compareByDescending { notification -> notification.createdAt?.toInstant() ?: Instant.MIN }),
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

    private data class FirstPageRequest(
        val category: NotificationCategory,
        val preserveContent: Boolean,
    )

    private companion object {
        const val PAGE_SIZE = 20
        const val VIEW_EFFECT_BUFFER_CAPACITY = 1
        const val RESUME_REFRESH_INTERVAL_MILLIS = 5_000L
    }
}
