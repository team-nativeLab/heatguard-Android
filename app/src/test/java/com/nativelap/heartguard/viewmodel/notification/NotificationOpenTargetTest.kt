package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationTarget
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationOpenTargetTest {
    @Test
    fun `기록 대상은 기록 상세를 연다`() {
        val openTarget = notification(NotificationTarget.Record("rec_01")).toOpenTarget()

        assertEquals(NotificationOpenTarget.RecordDetail("rec_01"), openTarget)
    }

    @Test
    fun `문의 답변 대상은 ID 유무와 관계없이 문의하기를 연다`() {
        assertEquals(
            NotificationOpenTarget.Inquiry,
            notification(NotificationTarget.InquiryAnswer("inq_01")).toOpenTarget(),
        )
        assertEquals(
            NotificationOpenTarget.Inquiry,
            notification(NotificationTarget.InquiryAnswer(inquiryId = null)).toOpenTarget(),
        )
    }

    @Test
    fun `긴급호출 대상은 진행 중 호출 화면으로 연결한다`() {
        assertEquals(
            NotificationOpenTarget.ActiveEmergencyCall,
            notification(NotificationTarget.EmergencyCall("call_01")).toOpenTarget(),
        )
    }

    @Test
    fun `대상이 없으면 이동하지 않는다`() {
        assertNull(notification(NotificationTarget.None).toOpenTarget())
    }

    private fun notification(target: NotificationTarget) =
        TeamNotification(
            notificationId = "ntf_01",
            type = NotificationType.UNKNOWN,
            category = NotificationCategory.UNKNOWN,
            title = "알림",
            target = target,
            isRead = false,
            createdAt = null,
            updatedAt = null,
        )
}
