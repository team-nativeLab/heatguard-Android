package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.record.mapper.toDomain
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordHistoryApiServiceTest {
    @Test
    fun `기록 목록은 date·cursor·limit 쿼리로 조회하고 실서버 항목 구조를 파싱한다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                // 2026-09-28 실서버 GET /api/v1/team/records 응답 구조(식별자만 예시 값)
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """{"success":true,"data":{"items":[{"recordId":"rec_01","type":"THERMOMETER",""" +
                                """"temperature":31.5,""" +
                                """"humidity":60.0,"apparentTemperature":35.2,"heatLevel":3,""" +
                                """"photoKeys":["teams/t/uploads/up_01.jpg"],""" +
                                """"memo":"메모","measuredAt":"2026-09-28T23:18:05+09:00","version":1,""" +
                                """"createdAt":"2026-09-28T14:18:05.642063+00:00",""" +
                                """"updatedAt":"2026-09-28T14:18:05.642063+00:00"}],""" +
                                """"page":{"nextCursor":"cur_2","hasMore":true}},"error":null,"meta":{"requestId":"req_01"}}""",
                        ).build(),
                )

                val response =
                    createService(server).getRecords(
                        date = "2026-09-28",
                        cursor = null,
                        limit = 100,
                    )

                val request = server.takeRequest()
                assertEquals("/api/v1/team/records", request.url.encodedPath)
                assertEquals("2026-09-28", request.url.queryParameter("date"))
                assertEquals("100", request.url.queryParameter("limit"))
                assertNull(request.url.queryParameter("cursor"))
                val recordPage = response.data ?: error("data missing")
                assertEquals("cur_2", recordPage.page?.nextCursor)
                val recordEntry = recordPage.items.single().toDomain()
                assertEquals(FieldRecordType.THERMOMETER, recordEntry.type)
                assertEquals(1, recordEntry.photoCount)
                assertEquals(23, recordEntry.measuredAt?.hour)
            } finally {
                server.close()
            }
        }

    @Test
    fun `기록 상세는 photoUrls를 파싱하고 모르는 유형은 null로 둔다`() =
        runBlocking {
            val server = MockWebServer()
            server.start()
            try {
                server.enqueue(
                    MockResponse
                        .Builder()
                        .code(200)
                        .body(
                            """{"success":true,"data":{"recordId":"rec_02","type":"NEW_TYPE",""" +
                                """"photoKeys":["k1","k2"],""" +
                                """"photoUrls":["https://example.test/1.jpg","https://example.test/2.jpg"],""" +
                                """"memo":"",""" +
                                """"measuredAt":"2026-09-28T10:00:00+09:00"},"error":null,"meta":{"requestId":"req_02"}}""",
                        ).build(),
                )

                val response = createService(server).getRecordDetail("rec_02")

                assertEquals("/api/v1/team/records/rec_02", server.takeRequest().url.encodedPath)
                val recordEntry = (response.data ?: error("data missing")).toDomain()
                assertNull(recordEntry.type)
                assertEquals(2, recordEntry.photoUrls.size)
                assertNull(recordEntry.memo)
            } finally {
                server.close()
            }
        }

    private fun createService(server: MockWebServer): RecordHistoryApiService =
        ApiRetrofitFactory(
            authenticatedApiClient = OkHttpClient(),
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = createUnauthenticatedTestSessionManager(),
            json = Json { ignoreUnknownKeys = true },
        ).createService(
            baseUrl = server.url("/").toString(),
            serviceClass = RecordHistoryApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
}
