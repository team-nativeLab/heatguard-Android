package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.TokenStorage
import kotlinx.coroutines.Dispatchers
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class BearerTokenAuthenticatorTest {
    @Test
    fun expiresSessionAndStopsRetryingWhenServerReturnsUnauthorized() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(MockResponse.Builder().code(401).build())

            val tokenStorage = FakeTokenStorage(accessToken = "expired-access-token")
            val sessionManager = SessionManager(
                tokenStorage = tokenStorage,
                ioDispatcher = Dispatchers.IO,
            )
            val client = OkHttpClient.Builder()
                .authenticator(BearerTokenAuthenticator(sessionManager))
                .build()
            val request = Request.Builder()
                .url(mockWebServer.url("/protected"))
                .build()

            // 401 응답을 받으면 Authenticator가 재시도 Request를 만들지 않고 그대로 실패를 반환해야 한다.
            client.newCall(request).execute().use { response ->
                assertEquals(401, response.code)
            }

            assertEquals(1, mockWebServer.requestCount)
            assertEquals(null, tokenStorage.accessToken)
            assertEquals(SessionState.Unauthenticated, sessionManager.sessionState.value)
        } finally {
            mockWebServer.close()
        }
    }

    private class FakeTokenStorage(
        var accessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
