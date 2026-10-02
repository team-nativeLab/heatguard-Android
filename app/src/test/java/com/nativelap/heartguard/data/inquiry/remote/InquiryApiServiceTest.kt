package com.nativelap.heartguard.data.inquiry.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.inquiry.dto.SubmitInquiryRequestDto
import com.nativelap.heartguard.data.inquiry.mapper.toDomain
import com.nativelap.heartguard.domain.inquiry.model.InquiryStatus
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
                sessionManager = createUnauthenticatedTestSessionManager(),
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

    @Test
    fun `문의 목록은 cursor·limit 쿼리로 조회하고 실서버 항목 구조를 파싱한다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            // 2026-09-28 실서버 GET /api/v1/team/inquiries 응답 구조(식별자만 예시 값)
            server.enqueue(
                MockResponse.Builder()
                    .code(200)
                    .body(
                        """{"success":true,"data":{"items":[{"inquiryId":"inq_01","title":"점검 문의","content":"내용",""" +
                            """"status":"ANSWERED","deliveryStatus":"DELIVERED","replies":[{"replyId":"rep_01","content":"답변"}],""" +
                            """"version":1,"createdAt":"2026-09-28T14:42:05.773386+00:00","updatedAt":"2026-09-28T14:42:05+00:00"}],""" +
                            """"page":{"nextCursor":null,"hasMore":false}},"error":null,"meta":{"requestId":"req_01"}}""",
                    )
                    .build(),
            )
            val service = ApiRetrofitFactory(
                authenticatedApiClient = OkHttpClient(),
                unauthenticatedApiClient = OkHttpClient(),
                sessionManager = createUnauthenticatedTestSessionManager(),
                json = Json { ignoreUnknownKeys = true },
            ).createService(
                baseUrl = server.url("/").toString(),
                serviceClass = InquiryApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )

            val response = service.getInquiries(
                cursor = null,
                limit = 50,
            )

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/team/inquiries", request.url.encodedPath)
            assertEquals("50", request.url.queryParameter("limit"))
            val inquirySummary = (response.data ?: error("data missing")).items.single().toDomain()
            assertEquals(InquiryStatus.ANSWERED, inquirySummary.status)
            assertEquals(1, inquirySummary.replyCount)
            assertEquals("점검 문의", inquirySummary.title)
        } finally {
            server.close()
        }
    }
}
