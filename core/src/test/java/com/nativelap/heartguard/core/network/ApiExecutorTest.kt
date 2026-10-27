package com.nativelap.heartguard.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiExecutorTest {
    private val apiExecutor = ApiExecutor()

    @Test
    fun `execute preserves status and server error details`() =
        runBlocking {
            val result =
                apiExecutor.execute {
                    throw httpException(
                        statusCode = 409,
                        body = """{"error":{"code":"VERSION_CONFLICT","message":"새 버전이 있습니다"}}""",
                    )
                }

            assertEquals(
                ApiResult.Failure(
                    ApiError.Http(
                        statusCode = 409,
                        errorCode = "VERSION_CONFLICT",
                        errorMessage = "새 버전이 있습니다",
                    ),
                ),
                result,
            )
        }

    @Test
    fun `execute preserves http failure when error body is empty or invalid`() =
        runBlocking {
            val emptyBody = apiExecutor.execute { throw httpException(statusCode = 401, body = null) }
            val invalidBody = apiExecutor.execute { throw httpException(statusCode = 502, body = "not-json") }

            assertEquals(ApiResult.Failure(ApiError.Http(statusCode = 401)), emptyBody)
            assertEquals(ApiResult.Failure(ApiError.Http(statusCode = 502)), invalidBody)
        }

    @Test
    fun `execute ignores error details when body exceeds limit`() =
        runBlocking {
            val oversizedBody = " ".repeat(ApiErrorCodeReader.MAX_ERROR_BODY_BYTES.toInt() + 1)

            val result =
                apiExecutor.execute {
                    throw httpException(statusCode = 500, body = oversizedBody)
                }

            assertEquals(ApiResult.Failure(ApiError.Http(statusCode = 500)), result)
        }

    @Test
    fun `execute maps network serialization and unexpected failures`() =
        runBlocking {
            val networkFailure = apiExecutor.execute { throw IOException("offline") }
            val serializationFailure = apiExecutor.execute { throw SerializationException("invalid payload") }
            val unexpectedFailure = apiExecutor.execute { throw IllegalStateException("unexpected") }

            assertEquals(ApiResult.Failure(ApiError.Network), networkFailure)
            assertEquals(ApiResult.Failure(ApiError.Serialization), serializationFailure)
            assertEquals(ApiResult.Failure(ApiError.Unknown), unexpectedFailure)
        }

    @Test
    fun `execute maps session and missing site failures`() =
        runBlocking {
            val sessionFailure = apiExecutor.execute { throw SessionChangedException() }
            val siteFailure = apiExecutor.execute { throw SiteNotSelectedException() }

            assertEquals(ApiResult.Failure(ApiError.SessionChanged), sessionFailure)
            assertEquals(ApiResult.Failure(ApiError.SiteNotSelected), siteFailure)
        }

    @Test
    fun `execute rethrows cancellation`() {
        val cancellation = CancellationException("cancelled")

        val thrown =
            assertFailsWith<CancellationException> {
                runBlocking {
                    apiExecutor.execute<Unit> { throw cancellation }
                }
            }

        assertEquals(cancellation, thrown)
    }

    private fun httpException(
        statusCode: Int,
        body: String?,
    ): HttpException {
        val responseBody = body?.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(statusCode, responseBody ?: "".toResponseBody()))
    }
}
