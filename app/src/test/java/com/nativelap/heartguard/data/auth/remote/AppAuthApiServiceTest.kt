package com.nativelap.heartguard.data.auth.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
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
                assertTrue(body.contains("\"email\":\"worker@example.com\""))
                assertTrue(body.contains("\"password\":\"${testPassword()}\""))
                assertTrue(body.contains("\"confirmPassword\""))
                assertTrue(body.contains("\"verificationCode\":\"123456\""))
                assertTrue(!body.contains("companyName") && !body.contains("memberName"))
            } finally {
                server.close()
            }
        }

    @Test
    fun `이메일 코드와 가입 오류 응답은 상태 코드와 서버 오류 코드를 보존한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(429)
                        .body(
                            """
                            {"success":false,"error":{"code":"EMAIL_CODE_RATE_LIMITED","message":"Too many requests"}}
                            """.trimIndent(),
                        ).build(),
                )
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(400)
                        .body(
                            """
                            {"success":false,"error":{"code":"INVALID_VERIFICATION_CODE","message":"Invalid code"}}
                            """.trimIndent(),
                        ).build(),
                )
                val remoteDataSource = createRemoteDataSource(server)

                val emailCodeResult = remoteDataSource.requestEmailCode("worker@example.com")
                val registrationResult =
                    remoteDataSource.register(
                        email = "worker@example.com",
                        password = testPassword(),
                        confirmPassword = testPassword(),
                        verificationCode = "123456",
                    )

                assertEquals(
                    ApiResult.Failure(ApiError.Http(statusCode = 429, errorCode = "EMAIL_CODE_RATE_LIMITED")),
                    emailCodeResult,
                )
                assertEquals(
                    ApiResult.Failure(ApiError.Http(statusCode = 400, errorCode = "INVALID_VERIFICATION_CODE")),
                    registrationResult,
                )
                assertEquals("/api/v1/auth/email-codes", server.takeRequest().url.encodedPath)
                assertEquals("/api/v1/auth/app/register", server.takeRequest().url.encodedPath)
            } finally {
                server.close()
            }
        }

    @Test
    fun `필수 인증코드 만료시각과 가입 데이터가 비어 있으면 직렬화 오류로 처리한다`() =
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
                            {"success":true,"data":{"expiresAt":" "},"error":null}
                            """.trimIndent(),
                        ).build(),
                )
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(201)
                        .body(
                            """
                            {"success":true,"data":{"userId":" ","email":"worker@example.com","createdAt":"2026-10-23T12:00:00+09:00"},"error":null}
                            """.trimIndent(),
                        ).build(),
                )
                val remoteDataSource = createRemoteDataSource(server)

                val emailCodeResult = remoteDataSource.requestEmailCode("worker@example.com")
                val registrationResult =
                    remoteDataSource.register(
                        email = "worker@example.com",
                        password = testPassword(),
                        confirmPassword = testPassword(),
                        verificationCode = "123456",
                    )

                assertEquals(ApiResult.Failure(ApiError.Serialization), emailCodeResult)
                assertEquals(ApiResult.Failure(ApiError.Serialization), registrationResult)
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
        createRetrofitFactory().createService(
            baseUrl = server.url("/").toString(),
            serviceClass = AppAuthApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )

    private fun createRemoteDataSource(server: MockWebServer): AppAuthRemoteDataSourceImpl {
        val retrofitFactory = createRetrofitFactory()
        val baseUrl = server.url("/").toString()
        return AppAuthRemoteDataSourceImpl(
            appAuthApiService =
                retrofitFactory.createService(
                    baseUrl = baseUrl,
                    serviceClass = AppAuthApiService::class.java,
                    authentication = ApiAuthentication.NONE,
                ),
            appAccountSessionApiService =
                retrofitFactory.createService(
                    baseUrl = baseUrl,
                    serviceClass = AppAccountSessionApiService::class.java,
                    authentication = ApiAuthentication.NONE,
                ),
            apiExecutor = ApiExecutor(),
        )
    }

    private fun createRetrofitFactory(): ApiRetrofitFactory =
        ApiRetrofitFactory(
            authenticatedApiClient = OkHttpClient(),
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = createUnauthenticatedTestSessionManager(),
            json = Json { ignoreUnknownKeys = true },
        )

    private fun testPassword(): String = CharArray(8) { 'x' }.concatToString()
}
