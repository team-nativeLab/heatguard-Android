package com.nativelap.heartguard.domain.notification.model

import java.time.OffsetDateTime

/** 작업자에게 공개된 팀 알림의 분류다. UNKNOWN은 서버가 새 분류를 추가한 경우를 보존한다. */
enum class NotificationCategory(
    val apiValue: String?,
) {
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

/** 알림이 가리키는 대상이다. 서버 응답의 type과 resourceId를 데이터 계층에서 구분해 만든다. */
sealed interface NotificationTarget {
    data class Record(
        val recordId: String,
    ) : NotificationTarget

    /** 긴급호출 확인 알림이다. [callId]는 서버 계약이 확인되지 않아 이동 판단에 쓰지 않고 보관만 한다. */
    data class EmergencyCall(
        val callId: String?,
    ) : NotificationTarget

    /** 문의 답변 알림이다. 문의 상세 화면이 없어 [inquiryId]가 없어도 문의 목록으로 이동할 수 있다. */
    data class InquiryAnswer(
        val inquiryId: String?,
    ) : NotificationTarget

    data object None : NotificationTarget
}

data class TeamNotification(
    val notificationId: String,
    val type: NotificationType,
    val category: NotificationCategory,
    val title: String,
    val target: NotificationTarget,
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
