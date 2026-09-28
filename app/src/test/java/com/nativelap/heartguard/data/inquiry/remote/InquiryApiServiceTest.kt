package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InquiryApiServiceTest {
    @Test
    fun `문의 등록은 명세된 제목과 내용을 보낸다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(201)
                    .body(
                        """{"success":true,"data":{"inquiryId":"inq_01","status":"OPEN","deliveryStatus":"PENDING","createdAt":"2026-09-28T10:00:00+09:00"},"error":null,"meta":{"requestId":"req_01"}}""",
                    )
                    .build(),
            )
            val service = ApiRetrofitFactory(
                authenticatedApiClient = OkHttpClient(),
                unauthenticatedApiClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
            ).createService(
                baseUrl = server.url("/").toString(),
                serviceClass = InquiryApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )

            val response = service.submitInquiry(
                SubmitInquiryRequestDto(
                    title = "점검 문의",
                    content = "테스트 문의 내용입니다.",
                ),
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/team/inquiries", request.url.encodedPath)
            val requestBody = request.body?.string(Charsets.UTF_8).orEmpty()
            assertTrue(requestBody.contains("\"title\":\"점검 문의\""))
            assertTrue(requestBody.contains("\"content\":\"테스트 문의 내용입니다.\""))
            assertEquals("inq_01", response.data?.inquiryId)
            assertEquals("PENDING", response.data?.deliveryStatus)
        } finally {
            server.close()
        }
    }
}
