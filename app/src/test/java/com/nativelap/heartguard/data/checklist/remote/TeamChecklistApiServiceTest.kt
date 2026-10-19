package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamChecklistApiServiceTest {
    @Test
    fun `체크리스트 조회는 날짜를 query로 보내고 빈 목록을 읽는다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """{"success":true,"data":{"date":"2026-10-19","items":[]},"error":null,"meta":{"requestId":"req_01"}}""",
                        ).build(),
                )

                val response = createService(server).getChecklist("2026-10-19")

                val request = server.takeRequest()
                assertEquals("GET", request.method)
                assertEquals("/api/v1/team/checklist", request.url.encodedPath)
                assertEquals("date=2026-10-19", request.url.encodedQuery)
                assertTrue(response.data?.items?.isEmpty() == true)
            } finally {
                server.close()
            }
        }

    @Test
    fun `체크 상태 저장은 item id 경로와 boolean body를 보낸다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """{"success":true,"data":{"itemId":"item_01","checked":false,"checkedAt":"2026-10-19T10:30:00+09:00"},"error":null,"meta":{"requestId":"req_01"}}""",
                        ).build(),
                )

                val response =
                    createService(server).setItemChecked(
                        itemId = "item_01",
                        request = UpdateChecklistItemRequestDto(checked = false),
                    )

                val request = server.takeRequest()
                assertEquals("PUT", request.method)
                assertEquals("/api/v1/team/checklist/items/item_01", request.url.encodedPath)
                assertEquals("""{"checked":false}""", request.body?.string(Charsets.UTF_8))
                assertEquals(false, response.data?.checked)
            } finally {
                server.close()
            }
        }

    private fun createService(server: MockWebServer): TeamChecklistApiService =
        ApiRetrofitFactory(
            authenticatedApiClient = OkHttpClient(),
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = createUnauthenticatedTestSessionManager(),
            json = Json { ignoreUnknownKeys = true },
        ).createService(
            baseUrl = server.url("/").toString(),
            serviceClass = TeamChecklistApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
}
