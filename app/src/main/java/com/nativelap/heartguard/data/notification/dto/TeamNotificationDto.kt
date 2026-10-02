package com.nativelap.heartguard.data.notification.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /api/v1/team/notifications 응답의 data 필드다. */
@Serializable
data class TeamNotificationPageDto(
    @SerialName("items")
    val items: List<TeamNotificationItemDto> = emptyList(),
    @SerialName("page")
    val page: TeamNotificationCursorDto? = null,
    @SerialName("unreadCount")
    val unreadCount: Int = 0,
    @SerialName("filteredUnreadCount")
    val filteredUnreadCount: Int = 0,
)

@Serializable
data class TeamNotificationCursorDto(
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("hasMore")
    val hasMore: Boolean = false,
)

@Serializable
data class TeamNotificationItemDto(
    @SerialName("notificationId")
    val notificationId: String,
    @SerialName("type")
    val type: String,
    @SerialName("category")
    val category: String,
    @SerialName("title")
    val title: String,
    @SerialName("resourceId")
    val resourceId: String? = null,
    @SerialName("read")
    val isRead: Boolean,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
)

/** PATCH /api/v1/team/notifications/{notificationId}/read 응답의 data 필드다. */
@Serializable
data class NotificationReadReceiptDto(
    @SerialName("notificationId")
    val notificationId: String,
    @SerialName("read")
    val isRead: Boolean,
    @SerialName("readAt")
    val readAt: String? = null,
)
