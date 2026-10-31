package com.nativelap.heartguard.viewmodel.notification

data class TeamNotificationUnreadUiState(
    val unreadCount: Int? = null,
) {
    val hasUnread: Boolean
        get() = (unreadCount ?: 0) > 0
}
