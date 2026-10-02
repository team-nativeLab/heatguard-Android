package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class BearerTokenAuthenticatorTest {
    @Test
    fun lateUnauthorizedResponseKeepsSameTokenNewSession() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse.Builder().code(401).build())
            val storage = FakeTokenStorage("same-token")
            val sessionManager = SessionManager(storage, Dispatchers.IO)
            runBlocking { sessionManager.initialize() }
            val previousGeneration = sessionManager.getSnapshot().generation
            val client = OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(sessionManager))
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    runBlocking { sessionManager.onLoginSucceeded(SessionToken("same-token")) }
                    response
                }
                .authenticator(BearerTokenAuthenticator(sessionManager))
                .build()
            client.newCall(Request.Builder().url(server.url("/protected")).build()).execute().use {
                assertEquals(401, it.code)
            }
            assertEquals("same-token", storage.accessToken)
            assertEquals(previousGeneration + 1L, sessionManager.getSnapshot().generation)
            assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
            assertEquals(1, server.requestCount)
        } finally {
            server.close()
        }
    }

    @Test
    fun unownedUnauthorizedRequestDoesNotExpireCurrentSession() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse.Builder().code(401).build())
            val storage = FakeTokenStorage("token")
            val sessionManager = SessionManager(storage, Dispatchers.IO)
            runBlocking { sessionManager.initialize() }
            val client = OkHttpClient.Builder()
                .authenticator(BearerTokenAuthenticator(sessionManager))
                .build()
            client.newCall(Request.Builder().url(server.url("/public")).build()).execute().close()
            assertEquals("token", storage.accessToken)
            assertEquals(SessionState.Authenticated, sessionManager.sessionState.value)
        } finally {
            server.close()
        }
    }

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
            runBlocking { sessionManager.initialize() }
            val client = OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(sessionManager))
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
