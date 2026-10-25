package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.domain.notification.model.NotificationTarget
import com.nativelap.heartguard.domain.notification.model.TeamNotification

/** 알림을 눌렀을 때 열 화면이다. 실제 목적지 전환은 Navigation 계층이 담당한다. */
sealed interface NotificationOpenTarget {
    data class RecordDetail(
        val recordId: String,
    ) : NotificationOpenTarget

    data object Inquiry : NotificationOpenTarget

    /** 진행 중인 긴급호출이 있을 때만 호출 중 화면을 연다. 새 호출을 시작하는 화면으로는 보내지 않는다. */
    data object ActiveEmergencyCall : NotificationOpenTarget
}

/** 데이터 계층이 구분한 알림 대상을 열 화면으로 바꾼다. 대상이 없으면 이동하지 않는다. */
internal fun TeamNotification.toOpenTarget(): NotificationOpenTarget? =
    when (val notificationTarget = target) {
        is NotificationTarget.Record -> NotificationOpenTarget.RecordDetail(notificationTarget.recordId)
        is NotificationTarget.InquiryAnswer -> NotificationOpenTarget.Inquiry
        is NotificationTarget.EmergencyCall -> NotificationOpenTarget.ActiveEmergencyCall
        NotificationTarget.None -> null
    }
