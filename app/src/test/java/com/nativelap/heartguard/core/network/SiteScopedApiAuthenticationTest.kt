package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.InMemoryTokenStorage
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.http.GET

class SiteScopedApiAuthenticationTest {
    @Test
    fun `현장 범위 앱 계정 요청에만 선택 ID가 포함된다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                repeat(3) { server.enqueue(successResponse()) }
                val appStorage = InMemoryTokenStorage()
                val appSession = createSessionManager(appStorage, SessionRole.APP_ACCOUNT)
                appStorage.saveSelectedSiteId("site_01")

                createService(server, appSession, ApiAuthentication.APP_ACCOUNT_SITE_BEARER).load()
                val scopedRequest = server.takeRequest()
                createService(server, appSession, ApiAuthentication.APP_ACCOUNT_BEARER).load()
                val unscopedAppRequest = server.takeRequest()

                val workerStorage = InMemoryTokenStorage()
                val workerSession = createSessionManager(workerStorage, SessionRole.TEAM_MEMBER)
                workerStorage.saveSelectedSiteId("site_01")
                createService(server, workerSession, ApiAuthentication.BEARER).load()
                val workerRequest = server.takeRequest()

                assertEquals("Bearer test-token", scopedRequest.headers["Authorization"])
                assertEquals("site_01", scopedRequest.headers["X-Site-Id"])
                assertNull(unscopedAppRequest.headers["X-Site-Id"])
                assertNull(workerRequest.headers["X-Site-Id"])
                assertEquals(3, server.requestCount)
            } finally {
                server.close()
            }
        }

    @Test
    fun `선택 현장이 없으면 현장 범위 요청을 전송하지 않는다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val sessionManager = createSessionManager(InMemoryTokenStorage(), SessionRole.APP_ACCOUNT)
                val result =
                    ApiExecutor().execute {
                        createService(server, sessionManager, ApiAuthentication.APP_ACCOUNT_SITE_BEARER).load()
                    }

                assertEquals(ApiResult.Failure(ApiError.SiteNotSelected), result)
                assertEquals(0, server.requestCount)
            } finally {
                server.close()
            }
        }

    @Test
    fun `요청 생성 후 현장이 바뀌어도 생성 당시 현장 ID를 사용한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(successResponse())
                val storage = InMemoryTokenStorage()
                val sessionManager = createSessionManager(storage, SessionRole.APP_ACCOUNT)
                storage.saveSelectedSiteId("site_a")
                val scopedClient =
                    OkHttpClient
                        .Builder()
                        .addInterceptor(BearerTokenInterceptor(sessionManager))
                        .addInterceptor(SelectedSiteIdInterceptor(sessionManager))
                        .build()
                val callFactory =
                    SessionBoundCallFactory(
                        delegateClient = scopedClient,
                        sessionManager = sessionManager,
                        requiredRole = SessionRole.APP_ACCOUNT,
                        requireSelectedSite = true,
                    )
                val call = callFactory.newCall(Request.Builder().url(server.url("api/v1/app/site-scoped-test")).build())
                storage.saveSelectedSiteId("site_b")

                call.execute()
                assertEquals("site_a", server.takeRequest().headers["X-Site-Id"])
            } finally {
                server.close()
            }
        }

    private fun createSessionManager(
        storage: InMemoryTokenStorage,
        role: SessionRole,
    ) = SessionManager(storage, Dispatchers.Unconfined).apply {
        runBlocking {
            onLoginSucceeded(SessionToken("test-token", role))
        }
    }

    private fun createService(
        server: MockWebServer,
        sessionManager: SessionManager,
        authentication: ApiAuthentication,
    ): ScopedTestApiService {
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
            serviceClass = ScopedTestApiService::class.java,
            authentication = authentication,
            allowRequestReplay = false,
        )
    }

    private fun successResponse(): MockResponse =
        MockResponse
            .Builder()
            .code(200)
            .body("""{"success":true,"data":"ok","error":null,"meta":null}""")
            .build()

    private interface ScopedTestApiService {
        @GET("api/v1/app/site-scoped-test")
        suspend fun load(): ApiEnvelope<String>
    }
}
