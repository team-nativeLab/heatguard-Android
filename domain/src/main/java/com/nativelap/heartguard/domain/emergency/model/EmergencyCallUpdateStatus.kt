package com.nativelap.heartguard.domain.emergency.model

/** 작업자가 긴급호출을 종료할 때 서버에 요청할 수 있는 상태다. */
enum class EmergencyCallUpdateStatus {
    CANCELLED,
    COMPLETED,
}
