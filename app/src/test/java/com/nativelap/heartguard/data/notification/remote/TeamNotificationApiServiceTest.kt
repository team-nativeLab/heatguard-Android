package com.nativelap.heartguard.data.notification.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TeamNotificationApiServiceTest {
    @Test
    fun `알림 목록은 분류 cursor limit로 조회해 페이지와 읽지 않은 수를 읽는다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(
                        """{"success":true,"data":{"items":[{"notificationId":"ntf_01","type":"RECORD_CREATED","category":"RECORD","title":"기록이 저장됐어요","resourceId":"rec_01","read":false,"createdAt":"2026-09-30T10:20:00+09:00","updatedAt":"2026-09-30T10:20:00+09:00"}],"page":{"nextCursor":"cursor+2","hasMore":true},"unreadCount":4,"filteredUnreadCount":2},"error":null,"meta":{"requestId":"req_01"}}""",
                    )
                    .build(),
            )

            val response = createService(server).getNotifications(
                category = "RECORD",
                limit = 20,
                cursor = "cursor+1",
            )

            val request = server.takeRequest()
            assertEquals("/api/v1/team/notifications", request.url.encodedPath)
            assertEquals("RECORD", request.url.queryParameter("category"))
            assertEquals("20", request.url.queryParameter("limit"))
            assertEquals("cursor+1", request.url.queryParameter("cursor"))
            val page = requireNotNull(response.data)
            assertEquals("ntf_01", page.items.single().notificationId)
            assertEquals("cursor+2", page.page?.nextCursor)
            assertEquals(4, page.unreadCount)
            assertEquals(2, page.filteredUnreadCount)
        } finally {
            server.close()
        }
    }

    @Test
    fun `알림 개별 읽음 요청은 PATCH를 보낸다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(
                        """{"success":true,"data":{"notificationId":"ntf_01","read":true,"readAt":"2026-09-30T10:22:00+09:00"},"error":null,"meta":{"requestId":"req_02"}}""",
                    )
                    .build(),
            )

            val response = createService(server).markNotificationRead("ntf_01")

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/team/notifications/ntf_01/read", request.url.encodedPath)
            assertEquals(true, requireNotNull(response.data).isRead)
            assertNotNull(response.data?.readAt)
        } finally {
            server.close()
        }
    }

    private fun createService(server: MockWebServer): TeamNotificationApiService = ApiRetrofitFactory(
        authenticatedApiClient = OkHttpClient(),
        unauthenticatedApiClient = OkHttpClient(),
        json = Json { ignoreUnknownKeys = true },
    ).createService(
        baseUrl = server.url("/").toString(),
        serviceClass = TeamNotificationApiService::class.java,
        authentication = ApiAuthentication.BEARER,
    )
}
