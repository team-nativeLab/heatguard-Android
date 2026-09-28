package com.nativelap.heartguard.core.session

import javax.inject.Inject

/** 작업자 팀 선택·토큰 발급 API가 명세에 없어 아직 팀 토큰을 제공할 수 없다. */
class DevTeamTokenProvider @Inject constructor() : TeamTokenProvider {
    override fun currentTeamToken(): String = error("작업자 팀 토큰 API가 준비되지 않았습니다.")
}
