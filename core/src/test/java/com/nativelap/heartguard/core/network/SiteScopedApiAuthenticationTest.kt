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
    fun `Team 현장 범위 요청에만 선택 ID가 포함된다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                repeat(2) { server.enqueue(successResponse()) }
                val teamStorage = InMemoryTokenStorage()
                val teamSession = createSessionManager(teamStorage, SessionRole.TEAM_MEMBER)
                teamStorage.saveSelectedSiteId("site_01")

                createService(server, teamSession, ApiAuthentication.TEAM_SITE_BEARER).load()
                val scopedRequest = server.takeRequest()
                createService(server, teamSession, ApiAuthentication.BEARER).load()
                val unscopedTeamRequest = server.takeRequest()

                assertEquals("Bearer test-token", scopedRequest.headers["Authorization"])
                assertEquals("site_01", scopedRequest.headers["X-Site-Id"])
                assertNull(unscopedTeamRequest.headers["X-Site-Id"])
                assertEquals(2, server.requestCount)
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
                val sessionManager = createSessionManager(InMemoryTokenStorage(), SessionRole.TEAM_MEMBER)
                val result =
                    ApiExecutor().execute {
                        createService(server, sessionManager, ApiAuthentication.TEAM_SITE_BEARER).load()
                    }

                assertEquals(ApiResult.Failure(ApiError.SiteNotSelected), result)
                assertEquals(0, server.requestCount)
            } finally {
                server.close()
            }
        }

    @Test
    fun `기존 앱 계정 세션은 Team 현장 요청에 사용할 수 없다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val legacySession = createSessionManager(InMemoryTokenStorage(), SessionRole.APP_ACCOUNT)
                val result =
                    ApiExecutor().execute {
                        createService(server, legacySession, ApiAuthentication.TEAM_SITE_BEARER).load()
                    }

                assertEquals(ApiResult.Failure(ApiError.SessionChanged), result)
                assertEquals(0, server.requestCount)
            } finally {
                server.close()
            }
        }

    @Test
    fun `요청 생성 후 전송 전에 선택된 최신 현장 ID를 읽는다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(successResponse())
                val storage = InMemoryTokenStorage()
                val sessionManager = createSessionManager(storage, SessionRole.TEAM_MEMBER)
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
                        requiredRole = SessionRole.TEAM_MEMBER,
                    )
                val call =
                    callFactory.newCall(
                        Request.Builder().url(server.url("api/v1/team/site-scoped-test")).build(),
                    )
                storage.saveSelectedSiteId("site_b")

                call.execute()
                assertEquals("site_b", server.takeRequest().headers["X-Site-Id"])
            } finally {
                server.close()
            }
        }

    @Test
    fun `세션 태그가 없는 현장 범위 요청은 전송하지 않는다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                val storage = InMemoryTokenStorage()
                val sessionManager = createSessionManager(storage, SessionRole.TEAM_MEMBER)
                storage.saveSelectedSiteId("site_01")
                val call =
                    OkHttpClient
                        .Builder()
                        .addInterceptor(SelectedSiteIdInterceptor(sessionManager))
                        .build()
                        .newCall(
                            Request
                                .Builder()
                                .url(server.url("api/v1/team/site-scoped-test"))
                                .build(),
                        )

                try {
                    call.execute()
                    throw AssertionError("A request without its owning session tag must fail closed")
                } catch (_: SessionChangedException) {
                    // Expected: site-scoped requests must be bound to their creating session.
                }
                assertEquals(0, server.requestCount)
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
        @GET("api/v1/team/site-scoped-test")
        suspend fun load(): ApiEnvelope<String>
    }
}
