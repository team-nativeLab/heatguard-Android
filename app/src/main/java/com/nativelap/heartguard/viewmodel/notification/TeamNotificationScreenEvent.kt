package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationCategory

sealed interface TeamNotificationScreenEvent {
    data object BackClicked : TeamNotificationScreenEvent

    data class CategorySelected(
        val category: NotificationCategory,
    ) : TeamNotificationScreenEvent

    data class NotificationClicked(
        val notificationId: String,
    ) : TeamNotificationScreenEvent

    data object MarkAllReadClicked : TeamNotificationScreenEvent

    data object RetryInitialLoad : TeamNotificationScreenEvent

    data object RetryRefresh : TeamNotificationScreenEvent

    data object LoadMore : TeamNotificationScreenEvent

    data object RetryLoadMore : TeamNotificationScreenEvent
}
