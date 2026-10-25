package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.createTestSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionRoleRoutingTest {
    @Test
    fun `앱 계정 세션은 작업자 API 요청을 생성하지 않는다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))
                val request = Request.Builder().url(server.url("/api/v1/team/checklist")).build()

                assertThrows(SessionChangedException::class.java) {
                    SessionBoundCallFactory(OkHttpClient(), sessionManager).newCall(request)
                }
                assertEquals(0, server.requestCount)
            } finally {
                server.close()
            }
        }

    @Test
    fun `탈퇴 완료 대기 세션은 역할이 남아도 token 없는 API 요청을 만들지 않는다`() =
        runBlocking {
            val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.TEAM_MEMBER))
            sessionManager.clearAccessTokenAfterWithdrawal()
            val request = Request.Builder().url("https://example.test/api/v1/team/checklist").build()

            assertThrows(SessionChangedException::class.java) {
                SessionBoundCallFactory(OkHttpClient(), sessionManager).newCall(request)
            }
            Unit
        }
}
