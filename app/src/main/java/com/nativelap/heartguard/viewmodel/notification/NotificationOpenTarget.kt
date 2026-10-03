package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification

/** 알림을 눌렀을 때 열 화면이다. 실제 목적지 전환은 Navigation 계층이 담당한다. */
sealed interface NotificationOpenTarget {
    data class RecordDetail(val recordId: String) : NotificationOpenTarget

    data object Inquiry : NotificationOpenTarget

    /** 진행 중인 긴급호출이 있을 때만 호출 중 화면을 연다. 새 호출을 시작하는 화면으로는 보내지 않는다. */
    data object ActiveEmergencyCall : NotificationOpenTarget
}

/** 알림 유형과 resourceId로 열 화면을 정한다. 기록 알림의 resourceId는 저장된 기록의 recordId다. */
internal fun TeamNotification.toOpenTarget(): NotificationOpenTarget? =
    when (type) {
        NotificationType.RECORD_CREATED -> {
            val recordId = resourceId?.trim()
            if (recordId.isNullOrEmpty()) {
                null
            } else {
                NotificationOpenTarget.RecordDetail(recordId)
            }
        }
        NotificationType.INQUIRY_ANSWERED -> NotificationOpenTarget.Inquiry
        NotificationType.EMERGENCY_ACKNOWLEDGED -> NotificationOpenTarget.ActiveEmergencyCall
        NotificationType.UNKNOWN -> null
    }
