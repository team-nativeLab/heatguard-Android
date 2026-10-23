package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.network.BearerTokenInterceptor
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.createTestSessionManager
import com.nativelap.heartguard.data.site.dto.ClaimAppSiteLinkRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSiteLinkApiServiceTest {
    @Test
    fun `현장 연동 요청은 앱 계정 인증과 명세 필드를 사용한다`() =
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
                              "data": {
                                "siteId": "site_01",
                                "siteName": "서울현장",
                                "linkedAt": "2026-10-23T12:00:00+09:00"
                              },
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )
                val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))

                createService(server, sessionManager).claim(
                    ClaimAppSiteLinkRequestDto(token = "y", managerPhone = "01012345678"),
                )
                val request = server.takeRequest()

                assertEquals("POST", request.method)
                assertEquals("/api/v1/app/links/claim", request.url.encodedPath)
                assertEquals("Bearer x", request.headers["Authorization"])
                assertEquals(
                    """{"token":"y","managerPhone":"01012345678"}""",
                    request.body?.string(Charsets.UTF_8),
                )
            } finally {
                server.close()
            }
        }

    @Test
    fun `현장 목록과 선택은 명세 경로를 사용한다`() =
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
                              "data": {
                                "items": [{
                                  "siteId": "site_01",
                                  "siteName": "서울현장",
                                  "managerName": "관리자",
                                  "linkedAt": "2026-10-23T12:00:00+09:00"
                                }]
                              },
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """
                            {
                              "success": true,
                              "data": {
                                "siteId": "site_01",
                                "siteName": "서울현장"
                              },
                              "error": null,
                              "meta": null
                            }
                            """.trimIndent(),
                        ).build(),
                )
                val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))
                val service = createService(server, sessionManager)

                val sites = service.getLinkedSites()
                val listRequest = server.takeRequest()
                val selected = service.selectSite("site_01")
                val selectRequest = server.takeRequest()

                assertEquals("GET", listRequest.method)
                assertEquals("/api/v1/app/sites", listRequest.url.encodedPath)
                assertTrue(sites.data?.items?.size == 1)
                assertEquals("POST", selectRequest.method)
                assertEquals("/api/v1/app/sites/site_01/select", selectRequest.url.encodedPath)
                assertTrue(selected.data?.siteId == "site_01")
            } finally {
                server.close()
            }
        }

    private fun createService(
        server: MockWebServer,
        sessionManager: SessionManager,
    ): AppSiteLinkApiService {
        val authenticatedClient =
            OkHttpClient
                .Builder()
                .addInterceptor(BearerTokenInterceptor(sessionManager))
                .build()
        return ApiRetrofitFactory(
            authenticatedApiClient = authenticatedClient,
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = sessionManager,
            json = Json { ignoreUnknownKeys = true },
        ).createService(
            baseUrl = server.url("/").toString(),
            serviceClass = AppSiteLinkApiService::class.java,
            authentication = ApiAuthentication.APP_ACCOUNT_BEARER,
            allowRequestReplay = false,
        )
    }
}
