package com.nativelap.heartguard.domain.auth.usecase

import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.emergency.usecase.EndActiveEmergencyCallResult
import com.nativelap.heartguard.domain.emergency.usecase.EndActiveEmergencyCallUseCase
import javax.inject.Inject

class TeamLogoutUseCase
    @Inject
    constructor(
        private val teamAuthRepository: TeamAuthRepository,
        private val endActiveEmergencyCallUseCase: EndActiveEmergencyCallUseCase,
    ) {
        /** 로그아웃했으면 true다. 진행 중 긴급 호출의 종료가 확인되지 않으면 세션을 유지하고 false를 돌려준다. */
        suspend operator fun invoke(): Boolean {
            // 서버 세션이 폐기되기 전에 진행 중인 긴급 호출을 끝낸다. 끝나지 않았다면 호출이 남으므로 로그아웃을 막는다.
            if (endActiveEmergencyCallUseCase() == EndActiveEmergencyCallResult.Unconfirmed) {
                return false
            }
            teamAuthRepository.logout()
            return true
        }
    }
