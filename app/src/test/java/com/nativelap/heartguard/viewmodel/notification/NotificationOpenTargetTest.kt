package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationOpenTargetTest {
    @Test
    fun `기록 알림은 resourceId를 recordId로 써서 기록 상세를 연다`() {
        val openTarget = notification(NotificationType.RECORD_CREATED, resourceId = " rec_01 ").toOpenTarget()

        assertEquals(NotificationOpenTarget.RecordDetail("rec_01"), openTarget)
    }

    @Test
    fun `resourceId가 없거나 비어 있는 기록 알림은 이동하지 않는다`() {
        assertNull(notification(NotificationType.RECORD_CREATED, resourceId = null).toOpenTarget())
        assertNull(notification(NotificationType.RECORD_CREATED, resourceId = "  ").toOpenTarget())
    }

    @Test
    fun `문의 답변과 긴급호출 확인 알림은 각각 문의와 진행 중 호출로 연결한다`() {
        assertEquals(
            NotificationOpenTarget.Inquiry,
            notification(NotificationType.INQUIRY_ANSWERED, resourceId = "inq_01").toOpenTarget(),
        )
        assertEquals(
            NotificationOpenTarget.ActiveEmergencyCall,
            notification(NotificationType.EMERGENCY_ACKNOWLEDGED, resourceId = "call_01").toOpenTarget(),
        )
    }

    @Test
    fun `알 수 없는 유형은 이동하지 않는다`() {
        assertNull(notification(NotificationType.UNKNOWN, resourceId = "x").toOpenTarget())
    }

    private fun notification(
        type: NotificationType,
        resourceId: String?,
    ) = TeamNotification(
        notificationId = "ntf_01",
        type = type,
        category = NotificationCategory.UNKNOWN,
        title = "알림",
        resourceId = resourceId,
        isRead = false,
        createdAt = null,
        updatedAt = null,
    )
}
