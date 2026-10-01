package com.nativelap.heartguard.data.notification.remote

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.NotificationReadAllReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto

interface TeamNotificationRemoteDataSource {
    suspend fun getNotifications(
        category: String,
        cursor: String?,
        limit: Int,
    ): ApiResult<TeamNotificationPageDto>

    suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceiptDto>

    suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceiptDto>
}
