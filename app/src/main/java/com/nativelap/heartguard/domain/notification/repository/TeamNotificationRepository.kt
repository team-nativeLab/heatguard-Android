package com.nativelap.heartguard.domain.notification.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage

interface TeamNotificationRepository {
    suspend fun getNotifications(
        category: NotificationCategory,
        cursor: String?,
        limit: Int,
    ): ApiResult<TeamNotificationPage>

    suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceipt>

    suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceipt>
}
