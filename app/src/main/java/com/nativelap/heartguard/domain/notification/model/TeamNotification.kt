package com.nativelap.heartguard.domain.notification.model

import java.time.OffsetDateTime

/** 작업자에게 공개된 팀 알림의 분류다. UNKNOWN은 서버가 새 분류를 추가한 경우를 보존한다. */
enum class NotificationCategory(val apiValue: String?) {
    ALL("ALL"),
    RECORD("RECORD"),
    EMERGENCY("EMERGENCY"),
    NOTICE("NOTICE"),
    UNKNOWN(null),
}

/** 알림을 발생시킨 서버 이벤트다. 미지 유형도 목록 전체 파싱을 실패시키지 않는다. */
enum class NotificationType {
    RECORD_CREATED,
    EMERGENCY_ACKNOWLEDGED,
    INQUIRY_ANSWERED,
    UNKNOWN,
}

data class TeamNotification(
    val notificationId: String,
    val type: NotificationType,
    val category: NotificationCategory,
    val title: String,
    val resourceId: String?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class TeamNotificationPage(
    val items: List<TeamNotification>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val unreadCount: Int,
    val filteredUnreadCount: Int,
)

data class NotificationReadReceipt(
    val notificationId: String,
    val isRead: Boolean,
    val readAt: OffsetDateTime?,
)

data class NotificationReadAllReceipt(
    val updatedCount: Int,
    val unreadCount: Int,
)
