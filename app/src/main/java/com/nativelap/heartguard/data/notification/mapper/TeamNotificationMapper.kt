package com.nativelap.heartguard.data.notification.mapper

import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationItemDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun TeamNotificationPageDto.toDomain(): TeamNotificationPage = TeamNotificationPage(
    items = items.map(TeamNotificationItemDto::toDomain),
    nextCursor = page?.nextCursor,
    hasMore = page?.hasMore == true,
    unreadCount = unreadCount.coerceAtLeast(0),
    filteredUnreadCount = filteredUnreadCount.coerceAtLeast(0),
)

internal fun TeamNotificationItemDto.toDomain(): TeamNotification = TeamNotification(
    notificationId = notificationId,
    type = type.toNotificationType(),
    category = category.toNotificationCategory(),
    title = title,
    resourceId = resourceId,
    isRead = isRead,
    createdAt = createdAt?.toOffsetDateTimeOrNull(),
    updatedAt = updatedAt?.toOffsetDateTimeOrNull(),
)

internal fun NotificationReadReceiptDto.toDomain(): NotificationReadReceipt = NotificationReadReceipt(
    notificationId = notificationId,
    isRead = isRead,
    readAt = readAt?.toOffsetDateTimeOrNull(),
)

private fun String.toNotificationCategory(): NotificationCategory = when (this) {
    "ALL" -> NotificationCategory.ALL
    "RECORD" -> NotificationCategory.RECORD
    "EMERGENCY" -> NotificationCategory.EMERGENCY
    "NOTICE" -> NotificationCategory.NOTICE
    else -> NotificationCategory.UNKNOWN
}

private fun String.toNotificationType(): NotificationType = when (this) {
    "RECORD_CREATED" -> NotificationType.RECORD_CREATED
    "EMERGENCY_ACKNOWLEDGED" -> NotificationType.EMERGENCY_ACKNOWLEDGED
    "INQUIRY_ANSWERED" -> NotificationType.INQUIRY_ANSWERED
    else -> NotificationType.UNKNOWN
}

private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? = try {
    OffsetDateTime.parse(this)
} catch (_: DateTimeParseException) {
    null
}
