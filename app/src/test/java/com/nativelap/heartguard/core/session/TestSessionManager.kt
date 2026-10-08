package com.nativelap.heartguard.core.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** 테스트에서 디스크 대신 메모리에 토큰을 두는 저장소다. */
class InMemoryTokenStorage : TokenStorage {
    var accessToken: String? = null

    override fun readAccessToken(): String? = accessToken

    override fun saveAccessToken(accessToken: String) {
        this.accessToken = accessToken
    }

    override fun clear() {
        accessToken = null
    }
}

/** Retrofit 서비스 계약 테스트처럼 세션 상태가 중요하지 않은 곳에서 쓰는 빈 세션 관리자다. */
fun createUnauthenticatedTestSessionManager(): SessionManager = createTestSessionManager(Dispatchers.Unconfined)

/** 로그인·로그아웃·계정 전환을 직접 조작하는 ViewModel 테스트용 세션 관리자다. */
fun createTestSessionManager(ioDispatcher: CoroutineDispatcher): SessionManager =
    SessionManager(
        tokenStorage = InMemoryTokenStorage(),
        ioDispatcher = ioDispatcher,
    )
