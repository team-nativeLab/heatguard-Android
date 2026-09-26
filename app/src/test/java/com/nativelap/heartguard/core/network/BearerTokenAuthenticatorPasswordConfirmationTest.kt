package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.core.session.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class BearerTokenAuthenticatorPasswordConfirmationTest {
    @Test
    fun keepsSessionWhenPasswordConfirmationRequestFailsWithInvalidCredentials() = runTest {
        val sessionResult = requestWithUnauthorizedResponse(
            errorCode = "INVALID_CREDENTIALS",
            isPasswordConfirmation = true,
        )

        assertEquals("valid-access-token", sessionResult.accessToken)
        assertEquals(SessionState.Authenticated, sessionResult.sessionState)
        // 본문을 peek만 했으므로 호출부가 오류 본문을 그대로 다시 읽을 수 있어야 한다.
        assertEquals("INVALID_CREDENTIALS", ApiErrorCodeReader.read(sessionResult.responseBody))
    }

    @Test
    fun expiresSessionWhenInvalidCredentialsComesFromUntaggedRequest() = runTest {
        val sessionResult = requestWithUnauthorizedResponse(
            errorCode = "INVALID_CREDENTIALS",
            isPasswordConfirmation = false,
        )

        assertEquals(null, sessionResult.accessToken)
        assertEquals(SessionState.Unauthenticated, sessionResult.sessionState)
    }

    @Test
    fun expiresSessionWhenPasswordConfirmationRequestFailsWithOtherCode() = runTest {
        val sessionResult = requestWithUnauthorizedResponse(
            errorCode = "UNAUTHORIZED",
            isPasswordConfirmation = true,
        )

        assertEquals(null, sessionResult.accessToken)
        assertEquals(SessionState.Unauthenticated, sessionResult.sessionState)
    }

    // 401 응답을 받은 뒤의 토큰·세션 상태와 호출부가 받은 응답 본문을 함께 돌려준다.
    private suspend fun requestWithUnauthorizedResponse(
        errorCode: String,
        isPasswordConfirmation: Boolean,
    ): SessionResult {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(
                MockResponse.Builder()
                    .code(401)
                    .body("""{"success":false,"data":null,"error":{"code":"$errorCode","message":"실패"}}""")
                    .build(),
            )

            val tokenStorage = FakeTokenStorage(accessToken = "valid-access-token")
            val sessionManager = SessionManager(
                tokenStorage = tokenStorage,
                ioDispatcher = Dispatchers.IO,
            )
            sessionManager.initialize()

            val client = OkHttpClient.Builder()
                .authenticator(BearerTokenAuthenticator(sessionManager))
                .build()
            val requestBuilder = Request.Builder()
                .url(mockWebServer.url("/api/v1/site/profile"))

            if (isPasswordConfirmation) {
                requestBuilder.tag(PasswordConfirmationRequest::class.java, PasswordConfirmationRequest)
            }

            val responseBody = client.newCall(requestBuilder.build()).execute().use { response ->
                assertEquals(401, response.code)
                response.body.string()
            }

            return SessionResult(
                accessToken = tokenStorage.accessToken,
                sessionState = sessionManager.sessionState.value,
                responseBody = responseBody,
            )
        } finally {
            mockWebServer.close()
        }
    }

    private data class SessionResult(
        val accessToken: String?,
        val sessionState: SessionState,
        val responseBody: String,
    )

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
