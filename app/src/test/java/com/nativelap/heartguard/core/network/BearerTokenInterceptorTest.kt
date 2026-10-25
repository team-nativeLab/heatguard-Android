package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BearerTokenInterceptorTest {
    @Test
    fun addsBearerAccessTokenToProtectedRequest() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(MockResponse.Builder().body("ok").build())

            val sessionManager = SessionManager(FakeTokenStorage("server-access-token"), Dispatchers.IO)
            runBlocking { sessionManager.initialize() }
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(BearerTokenInterceptor(sessionManager))
                    .build()
            val request =
                Request
                    .Builder()
                    .url(mockWebServer.url("/protected"))
                    .build()

            client.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
            }

            assertEquals(
                "Bearer server-access-token",
                mockWebServer.takeRequest().headers["Authorization"],
            )
        } finally {
            mockWebServer.close()
        }
    }

    @Test
    fun requestCreatedBeforeLogoutAndReloginIsNotSent() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(MockResponse.Builder().body("ok").build())
            val sessionManager = SessionManager(FakeTokenStorage("first-account-token"), Dispatchers.IO)
            runBlocking { sessionManager.initialize() }
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(BearerTokenInterceptor(sessionManager))
                    .build()
            val callFactory = SessionBoundCallFactory(client, sessionManager)
            val queuedCall =
                callFactory.newCall(
                    Request
                        .Builder()
                        .url(mockWebServer.url("/protected"))
                        .build(),
                )

            runBlocking {
                sessionManager.expireSession()
                sessionManager.onLoginSucceeded(SessionToken("second-account-token"))
            }

            assertThrows(SessionChangedException::class.java) {
                queuedCall.execute()
            }
            assertEquals(0, mockWebServer.requestCount)
        } finally {
            mockWebServer.close()
        }
    }

    @Test
    fun requestCreatedBeforeLogoutIsNotSentWithoutRelogin() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val sessionManager = SessionManager(FakeTokenStorage("account-token"), Dispatchers.IO)
            runBlocking { sessionManager.initialize() }
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(BearerTokenInterceptor(sessionManager))
                    .build()
            val queuedCall =
                SessionBoundCallFactory(client, sessionManager).newCall(
                    Request
                        .Builder()
                        .url(mockWebServer.url("/protected"))
                        .build(),
                )

            runBlocking { sessionManager.expireSession() }

            assertThrows(SessionChangedException::class.java) {
                queuedCall.execute()
            }
            assertEquals(0, mockWebServer.requestCount)
        } finally {
            mockWebServer.close()
        }
    }

    @Test
    fun sessionChangedFailureIsNotReportedAsNetworkError() {
        val apiResult =
            runBlocking {
                ApiExecutor().execute<Unit> { throw SessionChangedException() }
            }

        assertEquals(ApiResult.Failure(ApiError.SessionChanged), apiResult)
    }

    private class FakeTokenStorage(
        private val accessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
