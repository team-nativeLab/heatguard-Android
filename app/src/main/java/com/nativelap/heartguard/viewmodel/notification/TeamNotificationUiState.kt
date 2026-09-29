package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.TeamNotification

data class TeamNotificationUiState(
    val selectedCategory: NotificationCategory = NotificationCategory.ALL,
    val notifications: List<TeamNotification> = emptyList(),
    val isInitialLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasInitialError: Boolean = false,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasLoadMoreError: Boolean = false,
    val unreadCount: Int = 0,
    val filteredUnreadCount: Int = 0,
    val markingReadIds: Set<String> = emptySet(),
)
