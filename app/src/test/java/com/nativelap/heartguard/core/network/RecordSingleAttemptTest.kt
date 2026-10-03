package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.remote.RecordApiService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import java.io.IOException
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException

class RecordSingleAttemptTest {
    @Test
    fun `408 and retryable 503 do not replay a record POST`() {
        for (statusCode in listOf(408, 503)) {
            assertSingleAttempt(statusCode)
        }
    }

    @Test
    fun `307 and 308 do not redirect a record POST`() {
        for (statusCode in listOf(307, 308)) {
            assertSingleAttempt(statusCode)
        }
    }

    @Test
    fun `response connection loss does not replay a record POST`() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .onResponseStart(SocketEffect.CloseSocket())
                    .build(),
            )
            server.enqueue(MockResponse.Builder().code(500).build())
            assertThrows(IOException::class.java) {
                runBlocking {
                    createService(server).submitRecord(request())
                }
            }
            assertEquals(1, server.requestCount)
            assertEquals("POST", server.takeRequest().method)
        } finally {
            server.close()
        }
    }

    @Test
    fun `request generation mismatch sends no HTTP request`() {
        val server = MockWebServer()
        server.start()
        try {
            val service = createService(server)
            assertThrows(SessionChangedException::class.java) {
                runBlocking {
                    service.submitRecord(request(), RequestSessionGeneration(-1))
                }
            }
            assertEquals(0, server.requestCount)
        } finally {
            server.close()
        }
    }

    private fun assertSingleAttempt(statusCode: Int) {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(statusCode)
                    .addHeader("Retry-After", "0")
                    .addHeader("Location", server.url("/redirect"))
                    .build(),
            )
            server.enqueue(
                MockResponse.Builder()
                    .code(500)
                    .build(),
            )
            val failure = assertThrows(HttpException::class.java) {
                runBlocking {
                    createService(server).submitRecord(request())
                }
            }
            assertEquals(statusCode, failure.code())
            assertEquals(1, server.requestCount)
            assertEquals("/api/v1/team/records", server.takeRequest().url.encodedPath)
        } finally {
            server.close()
        }
    }

    private fun createService(server: MockWebServer): RecordApiService {
        return ApiRetrofitFactory(
            authenticatedApiClient = OkHttpClient(),
            unauthenticatedApiClient = OkHttpClient(),
            sessionManager = createUnauthenticatedTestSessionManager(),
            json = Json { ignoreUnknownKeys = true },
        ).createService(
            baseUrl = server.url("/").toString(),
            serviceClass = RecordApiService::class.java,
            authentication = ApiAuthentication.BEARER,
            allowRequestReplay = false,
        )
    }

    private fun request(): RecordRequestDto {
        return RecordRequestDto(
            type = "WORK",
            photoKeys = listOf("upload/photo"),
            measuredAt = "2026-10-04T09:00:00+09:00",
        )
    }
}
