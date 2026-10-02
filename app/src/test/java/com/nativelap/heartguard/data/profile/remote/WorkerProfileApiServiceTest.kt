package com.nativelap.heartguard.data.profile.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.core.network.PasswordConfirmationRequest
import com.nativelap.heartguard.data.profile.dto.ChangeWorkerPasswordRequestDto
import com.nativelap.heartguard.data.profile.dto.UpdateWorkerProfileRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkerProfileApiServiceTest {
    // 2026-09-28 실서버 GET·PATCH /api/v1/auth/team/me 응답 구조(식별자만 예시 값)
    private val profileResponseBody =
        """{"success":true,"data":{"userId":"usr_01","name":"홍길동","email":"worker01","phone":"010-0000-0000",""" +
            """"role":"TEAM_MEMBER","scopeId":"team_01","siteId":"site_01","teamId":"team_01","version":2,""" +
            """"expiresAt":"2026-09-28T22:43:47+00:00"},"error":null,"meta":{"requestId":"req_01"}}"""

    @Test
    fun `내 정보 조회는 me 경로를 GET으로 호출하고 응답을 파싱한다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(profileResponseBody)
                    .build(),
            )

            val response = createService(server).getWorkerProfile()

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/auth/team/me", request.url.encodedPath)
            assertEquals("홍길동", response.data?.name)
            assertEquals("worker01", response.data?.email)
        } finally {
            server.close()
        }
    }

    @Test
    fun `이름 수정은 PATCH 본문에 이름만 보낸다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(profileResponseBody)
                    .build(),
            )

            createService(server).updateWorkerProfile(
                UpdateWorkerProfileRequestDto(
                    name = "홍길동",
                ),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/auth/team/me", request.url.encodedPath)
            assertEquals("""{"name":"홍길동"}""", request.body?.string(Charsets.UTF_8))
        } finally {
            server.close()
        }
    }

    @Test
    fun `비밀번호 변경은 PUT 본문과 비밀번호 확인 태그를 보낸다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body("""{"success":true,"data":{"changedAt":"2026-09-28T14:35:53+00:00"},"error":null,"meta":null}""")
                    .build(),
            )

            val response = createService(server).changePassword(
                request = ChangeWorkerPasswordRequestDto(
                    currentPassword = "current1",
                    newPassword = "abcd1234",
                ),
                passwordConfirmationRequest = PasswordConfirmationRequest,
            )

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/auth/team/password", request.url.encodedPath)
            assertEquals(
                """{"currentPassword":"current1","newPassword":"abcd1234"}""",
                request.body?.string(Charsets.UTF_8),
            )
            assertEquals("2026-09-28T14:35:53+00:00", response.data?.changedAt)
        } finally {
            server.close()
        }
    }

    private fun createService(server: MockWebServer): WorkerProfileApiService = ApiRetrofitFactory(
        authenticatedApiClient = OkHttpClient(),
        unauthenticatedApiClient = OkHttpClient(),
        sessionManager = createUnauthenticatedTestSessionManager(),
        json = Json { ignoreUnknownKeys = true },
    ).createService(
        baseUrl = server.url("/").toString(),
        serviceClass = WorkerProfileApiService::class.java,
        authentication = ApiAuthentication.BEARER,
    )
}
