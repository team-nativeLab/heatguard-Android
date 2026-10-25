package com.nativelap.heartguard.data.site.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.network.BearerTokenInterceptor
import com.nativelap.heartguard.core.network.RequestSessionGeneration
import com.nativelap.heartguard.core.session.InMemoryTokenStorage
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
    fun `선택 현장 대시보드는 현장마다 별도 범위 헤더를 보낸다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """{"success":true,"data":{"summary":{"todayRecordCount":2,"activeEmergencyCount":0}}}""",
                        ).build(),
                )
                val storage = InMemoryTokenStorage()
                val sessionManager = SessionManager(storage, Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))
                storage.saveSelectedSiteId("site-a")
                val service = createDashboardService(server, sessionManager)

                assertEquals(
                    2,
                    service
                        .getDashboard(RequestSessionGeneration(sessionManager.getSnapshot().generation))
                        .data
                        ?.summary
                        ?.todayRecordCount,
                )
                val request = server.takeRequest()
                assertEquals("/api/v1/site/dashboard", request.url.encodedPath)
                assertEquals("Bearer x", request.headers["Authorization"])
                assertEquals("site-a", request.headers["X-Site-Id"])
            } finally {
                server.close()
            }
        }

    @Test
    fun `현장 해제는 앱 계정으로 해당 현장 링크만 삭제한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(MockResponse.Builder().code(204).build())
                val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))
                sessionManager.saveSelectedSiteIdForSession("selected_site", sessionManager.getSnapshot())

                createService(server, sessionManager).unlinkSite(
                    "site_01",
                    RequestSessionGeneration(sessionManager.getSnapshot().generation),
                )
                val request = server.takeRequest()

                assertEquals("DELETE", request.method)
                assertEquals("/api/v1/app/sites/site_01/link", request.url.encodedPath)
                assertEquals("Bearer x", request.headers["Authorization"])
                assertEquals(null, request.headers["X-Site-Id"])
            } finally {
                server.close()
            }
        }

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
                sessionManager.saveSelectedSiteIdForSession("selected_site", sessionManager.getSnapshot())

                createService(server, sessionManager).claim(
                    ClaimAppSiteLinkRequestDto(token = "y", managerPhone = "01012345678"),
                    RequestSessionGeneration(sessionManager.getSnapshot().generation),
                )
                val request = server.takeRequest()

                assertEquals("POST", request.method)
                assertEquals("/api/v1/app/links/claim", request.url.encodedPath)
                assertEquals("Bearer x", request.headers["Authorization"])
                assertEquals(null, request.headers["X-Site-Id"])
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
                sessionManager.saveSelectedSiteIdForSession("previous_site", sessionManager.getSnapshot())
                val service = createService(server, sessionManager)

                val owningSession = RequestSessionGeneration(sessionManager.getSnapshot().generation)
                val sites = service.getLinkedSites(owningSession)
                val listRequest = server.takeRequest()
                val selected = service.selectSite("site_01", owningSession)
                val selectRequest = server.takeRequest()

                assertEquals("GET", listRequest.method)
                assertEquals("/api/v1/app/sites", listRequest.url.encodedPath)
                assertEquals("Bearer x", listRequest.headers["Authorization"])
                assertEquals(null, listRequest.headers["X-Site-Id"])
                assertTrue(sites.data?.items?.size == 1)
                assertEquals("POST", selectRequest.method)
                assertEquals("/api/v1/app/sites/site_01/select", selectRequest.url.encodedPath)
                assertEquals("Bearer x", selectRequest.headers["Authorization"])
                assertEquals(null, selectRequest.headers["X-Site-Id"])
                assertEquals("", selectRequest.body?.string(Charsets.UTF_8))
                assertEquals("site_01", selected.data?.siteId)
                assertEquals("서울현장", selected.data?.siteName)
            } finally {
                server.close()
            }
        }

    @Test
    fun `선택 응답 ID 불일치와 이름 누락 또는 공백은 실패한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                listOf(
                    """{"siteId":"site_02","siteName":"다른 현장"}""",
                    """{"siteId":"site_01"}""",
                    """{"siteId":"site_01","siteName":""}""",
                ).forEach { responseData ->
                    server.enqueue(
                        MockResponse
                            .Builder()
                            .code(200)
                            .body("""{"success":true,"data":$responseData,"error":null,"meta":null}""")
                            .build(),
                    )
                }
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(500)
                        .body("server error")
                        .build(),
                )
                val sessionManager = createTestSessionManager(Dispatchers.Unconfined)
                sessionManager.onLoginSucceeded(SessionToken("x", SessionRole.APP_ACCOUNT))
                val remoteDataSource =
                    AppSiteLinkRemoteDataSourceImpl(
                        appSiteLinkApiService = createService(server, sessionManager),
                        appSiteDashboardApiService = createDashboardService(server, sessionManager),
                        apiExecutor = ApiExecutor(),
                    )

                repeat(3) {
                    assertEquals(
                        ApiResult.Failure(ApiError.Serialization),
                        remoteDataSource.selectSite("site_01", sessionManager.getSnapshot().generation),
                    )
                    assertEquals("", server.takeRequest().body?.string(Charsets.UTF_8))
                }
                assertEquals(
                    ApiResult.Failure(ApiError.Http(500)),
                    remoteDataSource.selectSite("site_01", sessionManager.getSnapshot().generation),
                )
                assertEquals("", server.takeRequest().body?.string(Charsets.UTF_8))
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

    private fun createDashboardService(
        server: MockWebServer,
        sessionManager: SessionManager,
    ): AppSiteDashboardApiService {
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
            serviceClass = AppSiteDashboardApiService::class.java,
            authentication = ApiAuthentication.APP_ACCOUNT_SITE_BEARER,
            allowRequestReplay = false,
        )
    }
}
