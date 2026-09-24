package com.nativelap.heartguard.domain.emergency.model

/** 긴급호출의 서버 상태다. NONE(호출 없음) → ACTIVE(대기 중) → ACKNOWLEDGED(관리자 확인함) 순으로 전이된다. */
enum class EmergencyCallState {
    NONE,
    ACTIVE,
    ACKNOWLEDGED,
}

/** 긴급호출 등록/상태조회 API가 공통으로 반환하는 값을 정리한 도메인 모델이다. */
data class EmergencyCallStatus(
    val callId: String?,
    val state: EmergencyCallState,
    val acknowledgedAt: String?,
)
