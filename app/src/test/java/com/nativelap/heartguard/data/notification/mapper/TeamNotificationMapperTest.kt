package com.nativelap.heartguard.data.notification.mapper

import com.nativelap.heartguard.data.notification.dto.TeamNotificationCursorDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationItemDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationType
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeamNotificationMapperTest {
    @Test
    fun `서버 이벤트 유형과 카테고리를 도메인 값으로 매핑한다`() {
        val pageDto = TeamNotificationPageDto(
            items = listOf(
                item("n-record", "RECORD_CREATED", "RECORD"),
                item("n-emergency", "EMERGENCY_ACKNOWLEDGED", "EMERGENCY"),
                item("n-inquiry", "INQUIRY_ANSWERED", "NOTICE"),
            ),
            page = TeamNotificationCursorDto(nextCursor = "next", hasMore = true),
            unreadCount = 4,
            filteredUnreadCount = 2,
        )

        val page = pageDto.toDomain()

        assertEquals(
            listOf(NotificationType.RECORD_CREATED, NotificationType.EMERGENCY_ACKNOWLEDGED, NotificationType.INQUIRY_ANSWERED),
            page.items.map { item -> item.type },
        )
        assertEquals(
            listOf(NotificationCategory.RECORD, NotificationCategory.EMERGENCY, NotificationCategory.NOTICE),
            page.items.map { item -> item.category },
        )
        assertEquals("next", page.nextCursor)
        assertEquals(true, page.hasMore)
        assertEquals(4, page.unreadCount)
        assertEquals(2, page.filteredUnreadCount)
        assertEquals(OffsetDateTime.parse("2026-09-30T10:20:00+09:00"), page.items.first().createdAt)
    }

    @Test
    fun `미지 이벤트와 잘못된 시각은 안전한 기본값으로 보존한다`() {
        val item = item("n-unknown", "FUTURE_EVENT", "FUTURE_CATEGORY").copy(createdAt = "invalid")
            .toDomain()

        assertEquals(NotificationType.UNKNOWN, item.type)
        assertEquals(NotificationCategory.UNKNOWN, item.category)
        assertNull(item.createdAt)
    }

    private fun item(id: String, type: String, category: String) = TeamNotificationItemDto(
        notificationId = id,
        type = type,
        category = category,
        title = "알림 제목",
        resourceId = "resource-1",
        isRead = false,
        createdAt = "2026-09-30T10:20:00+09:00",
        updatedAt = "2026-09-30T10:21:00+09:00",
    )
}
