package com.nativelap.heartguard.core.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** 테스트에서 디스크 대신 메모리에 토큰을 두는 저장소다. */
class InMemoryTokenStorage : TokenStorage {
    var accessToken: String? = null
    var sessionRole: SessionRole? = null
    var selectedSiteId: String? = null

    override fun readAccessToken(): String? = accessToken

    override fun saveAccessToken(accessToken: String) {
        this.accessToken = accessToken
        sessionRole = SessionRole.TEAM_MEMBER
    }

    override fun readSessionRole(): SessionRole? = sessionRole

    override fun saveSession(
        accessToken: String,
        role: SessionRole,
    ) {
        this.accessToken = accessToken
        sessionRole = role
        selectedSiteId = null
    }

    override fun readSelectedSiteId(): String? = selectedSiteId

    override fun saveSelectedSiteId(siteId: String) {
        selectedSiteId = siteId
    }

    override fun clearSelectedSiteId() {
        selectedSiteId = null
    }

    override fun clear() {
        accessToken = null
        sessionRole = null
        selectedSiteId = null
    }
}

/** Retrofit 서비스 계약 테스트처럼 세션 상태가 중요하지 않은 곳에서 쓰는 빈 세션 관리자다. */
fun createUnauthenticatedTestSessionManager(): SessionManager = createTestSessionManager(Dispatchers.Unconfined)

/** TEAM_MEMBER 인증 API 계약 테스트에서 세션 역할 가드를 통과시키는 테스트 세션 관리자다. */
fun createTeamMemberTestSessionManager(): SessionManager =
    createTestSessionManager(Dispatchers.Unconfined).apply {
        kotlinx.coroutines.runBlocking {
            onLoginSucceeded(SessionToken(accessToken = "x"))
        }
    }

/** 로그인·로그아웃·계정 전환을 직접 조작하는 ViewModel 테스트용 세션 관리자다. */
fun createTestSessionManager(ioDispatcher: CoroutineDispatcher): SessionManager =
    SessionManager(
        tokenStorage = InMemoryTokenStorage(),
        ioDispatcher = ioDispatcher,
    )
