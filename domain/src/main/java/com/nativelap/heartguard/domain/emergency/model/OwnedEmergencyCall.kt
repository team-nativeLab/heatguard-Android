package com.nativelap.heartguard.domain.emergency.model

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이 앱 실행 중 이 기기가 시작하거나 이어받은 긴급 호출 ID를 메모리에만 보관한다.
 * 팀의 최신 호출은 다른 작업자의 것일 수 있으므로 로그아웃 시 종료 대상을 이 값으로만 제한한다.
 * 프로세스가 종료되면 사라진다(복원 후에는 호출 시트를 닫고, 진행 중 호출은 백그라운드 취소로 정리된다).
 */
@Singleton
class OwnedEmergencyCall
    @Inject
    constructor() {
        @Volatile
        private var ownedCallId: String? = null

        fun current(): String? = ownedCallId

        fun own(callId: String) {
            ownedCallId = callId
        }

        /** [callId]가 현재 소유 호출일 때만 비운다. 다른 호출이 이미 소유 중이면 그대로 둔다. */
        fun release(callId: String) {
            if (ownedCallId == callId) {
                ownedCallId = null
            }
        }
    }
