package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.RecordResponseDto
import com.nativelap.heartguard.data.record.dto.UploadRequestDto
import com.nativelap.heartguard.data.record.dto.UploadResponseDto
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordRemoteDataSourceUploadTest {

    @Test
    fun `presigned URL에 서버가 준 헤더로 PUT하고 2xx면 성공으로 본다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse.Builder().code(200).build())

            val uploadResult = createDataSource().uploadToPresignedUrl(
                uploadUrl = server.url("/teams/team_01/uploads/up_01.jpg?signature=abc").toString(),
                requiredHeaders = mapOf("Content-Type" to "image/jpeg"),
                photoContent = "photo-bytes".toByteArray().toRequestBody(null),
            )

            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("image/jpeg", request.headers["Content-Type"])
            assertEquals("photo-bytes", request.body?.string(Charsets.UTF_8))
            assertEquals(ApiResult.Success(Unit), uploadResult)
        } finally {
            server.close()
        }
    }

    @Test
    fun `서명이 만료돼 403이면 네트워크 오류가 아니라 HTTP 오류로 돌려준다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse.Builder().code(403).build())

            val uploadResult = createDataSource().uploadToPresignedUrl(
                uploadUrl = server.url("/upload").toString(),
                requiredHeaders = emptyMap(),
                photoContent = "photo-bytes".toByteArray().toRequestBody(null),
            )

            assertEquals(ApiResult.Failure(ApiError.Http(statusCode = 403)), uploadResult)
        } finally {
            server.close()
        }
    }

    @Test
    fun `업로드 서버에 연결할 수 없으면 네트워크 오류다`() = runBlocking {
        val server = MockWebServer()
        server.start()
        val unreachableUrl = server.url("/upload").toString()
        server.close()

        val uploadResult = createDataSource().uploadToPresignedUrl(
            uploadUrl = unreachableUrl,
            requiredHeaders = emptyMap(),
            photoContent = "photo-bytes".toByteArray().toRequestBody(null),
        )

        assertEquals(ApiResult.Failure(ApiError.Network), uploadResult)
    }

    private fun createDataSource() = RecordRemoteDataSourceImpl(
        uploadApiService = object : UploadApiService {
            override suspend fun issueUploadUrls(request: UploadRequestDto): ApiEnvelope<UploadResponseDto> =
                error("not used")
        },
        recordApiService = object : RecordApiService {
            override suspend fun submitRecord(request: RecordRequestDto): ApiEnvelope<RecordResponseDto> =
                error("not used")
        },
        externalUploadService = ExternalUploadService(OkHttpClient()),
        apiExecutor = ApiExecutor(),
    )
}
