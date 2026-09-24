package com.nativelap.heartguard.core.session

/** 현재 팀의 TEAM_TOKEN(현장 QR에 담긴 불투명 토큰)을 제공한다.
 * API 명세서상 TEAM_TOKEN은 이메일 로그인 세션([SessionManager])과 인증 모델이 달라
 * 별도 인터페이스로 분리한다 — 로그인 상태와 무관하게 팀 토큰만 알면 되는 API(홈 현장페이지
 * 조회, 긴급호출, 기록 등록 등)가 이 Provider만 의존하게 하기 위함이다. */
interface TeamTokenProvider {
    fun currentTeamToken(): String
}
