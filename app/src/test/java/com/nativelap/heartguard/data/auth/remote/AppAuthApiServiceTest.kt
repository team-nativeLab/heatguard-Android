package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.auth.dto.AppAccountLoginRequestDto
import com.nativelap.heartguard.data.auth.dto.RegisterAppAccountDto
import com.nativelap.heartguard.data.auth.dto.RequestAppEmailCodeDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAuthApiServiceTest {
    @Test
    fun `공통 인증코드 발송은 이메일만 공개 API로 보낸다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(202)
                        .body(
                            """
                            {
                              "success": true,
                              "data": {"expiresAt": "2026-10-23T12:10:00+09:00"},
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )

                val response =
                    createService(server).requestEmailCode(
                        RequestAppEmailCodeDto("worker@example.com"),
                    )
                val request = server.takeRequest()

                assertEquals("POST", request.method)
                assertEquals("/api/v1/auth/email-codes", request.url.encodedPath)
                assertEquals("""{"email":"worker@example.com"}""", request.body?.string(Charsets.UTF_8))
                assertTrue(response.data?.expiresAt?.isNotBlank() == true)
            } finally {
                server.close()
            }
        }

    @Test
    fun `가입은 명세 필드만 보내고 이메일 인증코드를 포함한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(201)
                        .body(
                            """
                            {
                              "success": true,
                              "data": {
                                "userId": "user_01",
                                "email": "worker@example.com",
                                "createdAt": "2026-10-23T12:00:00+09:00"
                              },
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )

                createService(server).register(
                    RegisterAppAccountDto(
                        email = "worker@example.com",
                        password = testPassword(),
                        confirmPassword = testPassword(),
                        verificationCode = "123456",
                    ),
                )
                val request = server.takeRequest()
                val body = request.body?.string(Charsets.UTF_8).orEmpty()

                assertEquals("POST", request.method)
                assertEquals("/api/v1/auth/app/register", request.url.encodedPath)
                assertTrue(body.contains("\"confirmPassword\""))
                assertTrue(body.contains("\"verificationCode\":\"123456\""))
                assertTrue(!body.contains("companyName") && !body.contains("memberName"))
            } finally {
                server.close()
            }
        }

    @Test
    fun `앱 계정 로그인은 앱 전용 로그인 경로를 사용한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """
                            {
                              "success": true,
                              "data": {"accessToken": "app-access"},
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )

                val response =
                    createService(server).login(
                        AppAccountLoginRequestDto("worker@example.com", testPassword()),
                    )
                val request = server.takeRequest()

                assertEquals("POST", request.method)
                assertEquals("/api/v1/auth/app/login", request.url.encodedPath)
                assertTrue(response.data?.accessToken?.isNotBlank() == true)
            } finally {
                server.close()
            }
        }

    private fun createService(server: MockWebServer): AppAuthApiService =
        ApiRetrofitFactory(
            authenticatedApiClient = OkHttpClient(),
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = createUnauthenticatedTestSessionManager(),
            json = Json { ignoreUnknownKeys = true },
        ).createService(
            baseUrl = server.url("/").toString(),
            serviceClass = AppAuthApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )

    private fun testPassword(): String = CharArray(8) { 'x' }.concatToString()
}
