package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.BuildConfig
import javax.inject.Inject

/** [TeamTokenProvider]의 개발용 임시 구현이다.
 * TODO: 실제 팀 토큰 발급/전달 방식(QR 스캔 등)이 정해지면 이 구현을 교체한다. 지금은
 * 백엔드팀이 발급한 테스트 팀 토큰이 없어 [BuildConfig.DEV_TEAM_TOKEN] placeholder를 반환한다. */
class DevTeamTokenProvider @Inject constructor() : TeamTokenProvider {
    override fun currentTeamToken(): String = BuildConfig.DEV_TEAM_TOKEN
}
