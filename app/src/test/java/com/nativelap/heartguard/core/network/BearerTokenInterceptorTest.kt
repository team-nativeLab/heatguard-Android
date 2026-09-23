package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.TokenStorage
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class BearerTokenInterceptorTest {
    @Test
    fun addsBearerAccessTokenToProtectedRequest() {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(MockResponse.Builder().body("ok").build())

            val client = OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(FakeTokenStorage("server-access-token")))
                .build()
            val request = Request.Builder()
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

    private class FakeTokenStorage(
        private val accessToken: String?,
    ) : TokenStorage {
        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }
}
