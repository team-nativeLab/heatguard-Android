package com.nativelap.heartguard.data.notification.mapper

import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.NotificationReadAllReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationItemDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationTarget
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
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

internal fun TeamNotificationItemDto.toDomain(): TeamNotification {
    val notificationType = type.toNotificationType()
    return TeamNotification(
        notificationId = notificationId,
        type = notificationType,
        category = category.toNotificationCategory(),
        title = title,
        target = notificationType.toTarget(resourceId),
        isRead = isRead,
        createdAt = createdAt?.toOffsetDateTimeOrNull(),
        updatedAt = updatedAt?.toOffsetDateTimeOrNull(),
    )
}

internal fun NotificationReadReceiptDto.toDomain(): NotificationReadReceipt = NotificationReadReceipt(
    notificationId = notificationId,
    isRead = isRead,
    readAt = readAt?.toOffsetDateTimeOrNull(),
)

internal fun NotificationReadAllReceiptDto.toDomain(): NotificationReadAllReceipt = NotificationReadAllReceipt(
    updatedCount = updatedCount.coerceAtLeast(0),
    unreadCount = unreadCount.coerceAtLeast(0),
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

/** 명세에 있는 type만 대상으로 매핑한다. 기록 알림의 resourceId는 저장된 기록의 recordId다.
 * 미지 type은 category가 같아도 의미를 알 수 없으므로 이동 대상을 만들지 않는다. */
private fun NotificationType.toTarget(resourceId: String?): NotificationTarget {
    val normalizedResourceId = resourceId
        ?.trim()
        ?.takeIf { trimmedId -> trimmedId.isNotEmpty() }
    return when (this) {
        NotificationType.RECORD_CREATED -> {
            if (normalizedResourceId == null) {
                NotificationTarget.None
            } else {
                NotificationTarget.Record(normalizedResourceId)
            }
        }
        NotificationType.EMERGENCY_ACKNOWLEDGED -> NotificationTarget.EmergencyCall(normalizedResourceId)
        NotificationType.INQUIRY_ANSWERED -> NotificationTarget.InquiryAnswer(normalizedResourceId)
        NotificationType.UNKNOWN -> NotificationTarget.None
    }
}

private fun String.toOffsetDateTimeOrNull(): OffsetDateTime? = try {
    OffsetDateTime.parse(this)
} catch (_: DateTimeParseException) {
    null
}
