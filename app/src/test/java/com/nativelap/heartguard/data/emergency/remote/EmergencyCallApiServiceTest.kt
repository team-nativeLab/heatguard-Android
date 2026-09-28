package com.nativelap.heartguard.data.emergency.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.emergency.dto.UpdateEmergencyCallStatusRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyCallApiServiceTest {
    @Test
    fun `상태 변경은 세션 경로와 상태 본문으로 요청한다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .body(
                        """{"success":true,"data":{"callId":"call_01","status":"CANCELLED","updatedAt":"2026-09-28T10:00:00+09:00","cancelledAt":"2026-09-28T10:00:00+09:00"},"error":null,"meta":{"requestId":"req_01"}}""",
                    )
                    .build(),
            )
            val service = ApiRetrofitFactory(
                authenticatedApiClient = OkHttpClient(),
                unauthenticatedApiClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
            ).createService(
                baseUrl = server.url("/").toString(),
                serviceClass = EmergencyCallApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )

            val response = service.updateEmergencyCallStatus(
                callId = "call_01",
                request = UpdateEmergencyCallStatusRequestDto(status = "CANCELLED"),
            )

            val request = server.takeRequest()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/team/emergency-calls/call_01", request.url.encodedPath)
            assertTrue(
                request.body?.string(Charsets.UTF_8)?.contains("\"status\":\"CANCELLED\"") == true,
            )
            assertEquals("CANCELLED", response.data?.status)
        } finally {
            server.close()
        }
    }
}
