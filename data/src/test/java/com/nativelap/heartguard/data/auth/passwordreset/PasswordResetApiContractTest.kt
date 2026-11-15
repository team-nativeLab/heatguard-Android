package com.nativelap.heartguard.data.auth.passwordreset

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.network.di.NetworkModule
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetCompletionResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetEmailCodeResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.dto.PasswordResetVerificationResponseDto
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetApiService
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetRemoteDataSourceImpl
import com.nativelap.heartguard.data.auth.passwordreset.repository.PasswordResetRepositoryImpl
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetEmailCode
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetVerification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

class PasswordResetApiContractTest {
    @Test
    fun requestEmailCodeUsesEndpointEmailFieldAndReturnsExpiration() {
        var result: ApiResult<PasswordResetEmailCodeResponseDto>? = null
        val request =
            serveResponse(
                statusCode = 202,
                responseBody =
                    """{"success":true,"data":{"expiresAt":"2026-11-15T12:10:00Z"},"error":null}""",
            ) { baseUrl ->
                result = remoteDataSource(baseUrl).requestEmailCode("a@b.co")
            }

        assertEquals("POST /api/v1/auth/password-reset/email-codes HTTP/1.1", request.requestLine)
        assertEquals(setOf("email"), request.bodyJson().keys)
        assertEquals("a@b.co", request.bodyJson()["email"]?.jsonPrimitive?.content)
        assertEquals(
            ApiResult.Success(PasswordResetEmailCodeResponseDto("2026-11-15T12:10:00Z")),
            result,
        )
        assertHasNoAuthenticationOrScopeHeaders(request)
    }

    @Test
    fun verifyEmailCodePreservesLeadingZeroAndReturnsResetToken() {
        var result: ApiResult<PasswordResetVerificationResponseDto>? = null
        val request =
            serveResponse(
                responseBody =
                    """{"success":true,"data":{"resetToken":"x","expiresAt":"2026-11-15T12:10:00Z"},"error":null}""",
            ) { baseUrl ->
                result = remoteDataSource(baseUrl).verifyEmailCode("a@b.co", "012345")
            }

        assertEquals("POST /api/v1/auth/password-reset/email-codes/verify HTTP/1.1", request.requestLine)
        assertEquals(setOf("email", "verificationCode"), request.bodyJson().keys)
        assertEquals("012345", request.bodyJson()["verificationCode"]?.jsonPrimitive?.content)
        assertEquals("a@b.co", request.bodyJson()["email"]?.jsonPrimitive?.content)
        assertEquals(
            ApiResult.Success(PasswordResetVerificationResponseDto("x", "2026-11-15T12:10:00Z")),
            result,
        )
        assertHasNoAuthenticationOrScopeHeaders(request)
    }

    @Test
    fun resetPasswordUsesAllRequestFieldsAndReturnsCompletion() {
        var result: ApiResult<PasswordResetCompletionResponseDto>? = null
        val request =
            serveResponse(
                responseBody =
                    """{"success":true,"data":{"passwordChanged":true,"changedAt":"2026-11-15T12:00:00Z"},"error":null}""",
            ) { baseUrl ->
                result =
                    remoteDataSource(baseUrl).resetPassword(
                        email = "a@b.co",
                        token = "x",
                        password = "a1",
                        confirmation = "a1",
                    )
            }

        assertEquals("POST /api/v1/auth/password-reset HTTP/1.1", request.requestLine)
        assertEquals(setOf("email", "resetToken", "newPassword", "confirmPassword"), request.bodyJson().keys)
        assertEquals("a@b.co", request.bodyJson()["email"]?.jsonPrimitive?.content)
        assertEquals("x", request.bodyJson()["resetToken"]?.jsonPrimitive?.content)
        assertEquals("a1", request.bodyJson()["newPassword"]?.jsonPrimitive?.content)
        assertEquals("a1", request.bodyJson()["confirmPassword"]?.jsonPrimitive?.content)
        assertEquals(
            ApiResult.Success(PasswordResetCompletionResponseDto(true, "2026-11-15T12:00:00Z")),
            result,
        )
        assertHasNoAuthenticationOrScopeHeaders(request)
    }

    @Test
    fun malformedExpirationAndRequiredResponseFieldsAreSerializationFailures() {
        var emailCodeResult: ApiResult<*>? = null
        serveResponse(
            responseBody = """{"success":true,"data":{"expiresAt":"bad"},"error":null}""",
        ) { baseUrl ->
            emailCodeResult = remoteDataSource(baseUrl).requestEmailCode("a@b.co")
        }
        assertEquals(ApiResult.Failure(ApiError.Serialization), emailCodeResult)

        var verificationResult: ApiResult<*>? = null
        serveResponse(
            responseBody = """{"success":true,"data":{"expiresAt":"2026-11-15T12:10:00Z"},"error":null}""",
        ) { baseUrl ->
            verificationResult = remoteDataSource(baseUrl).verifyEmailCode("a@b.co", "012345")
        }
        assertEquals(ApiResult.Failure(ApiError.Serialization), verificationResult)

        var completionResult: ApiResult<*>? = null
        serveResponse(
            responseBody =
                """{"success":true,"data":{"passwordChanged":false,"changedAt":"2026-11-15T12:00:00Z"},"error":null}""",
        ) { baseUrl ->
            completionResult =
                remoteDataSource(baseUrl).resetPassword(
                    email = "a@b.co",
                    token = "x",
                    password = "a1",
                    confirmation = "a1",
                )
        }
        assertEquals(ApiResult.Failure(ApiError.Serialization), completionResult)
    }

    @Test
    fun invalidCodeAndResetTokenErrorsMapToDomainFailures() {
        var invalidCodeResult: PasswordResetResult<PasswordResetVerification>? = null
        serveResponse(
            statusCode = 400,
            responseBody = """{"success":false,"error":{"code":"INVALID_VERIFICATION_CODE"}}""",
        ) { baseUrl ->
            invalidCodeResult =
                PasswordResetRepositoryImpl(remoteDataSource(baseUrl)).verifyEmailCode("a@b.co", "012345")
        }
        assertEquals(PasswordResetResult.Failure(PasswordResetError.InvalidCode), invalidCodeResult)

        var invalidTokenResult: PasswordResetResult<*>? = null
        serveResponse(
            statusCode = 400,
            responseBody = """{"success":false,"error":{"code":"INVALID_RESET_TOKEN"}}""",
        ) { baseUrl ->
            invalidTokenResult =
                PasswordResetRepositoryImpl(remoteDataSource(baseUrl)).resetPassword(
                    email = "a@b.co",
                    resetToken = "x",
                    newPassword = "a1",
                    confirmPassword = "a1",
                )
        }
        assertEquals(PasswordResetResult.Failure(PasswordResetError.InvalidToken), invalidTokenResult)
    }

    @Test
    fun requestLimitAndServiceFailureStatusesMapToDomainFailures() {
        var rateLimitedResult: PasswordResetResult<PasswordResetEmailCode>? = null
        serveResponse(
            statusCode = 429,
            responseBody = """{"success":false,"error":{"code":"RATE_LIMITED"}}""",
        ) { baseUrl ->
            rateLimitedResult =
                PasswordResetRepositoryImpl(remoteDataSource(baseUrl)).requestEmailCode("a@b.co")
        }
        assertEquals(PasswordResetResult.Failure(PasswordResetError.RateLimited), rateLimitedResult)

        var serviceUnavailableResult: PasswordResetResult<*>? = null
        serveResponse(
            statusCode = 503,
            responseBody = """{"success":false,"error":{"code":"SERVICE_DEGRADED"}}""",
        ) { baseUrl ->
            serviceUnavailableResult =
                PasswordResetRepositoryImpl(remoteDataSource(baseUrl)).requestEmailCode("a@b.co")
        }
        assertEquals(PasswordResetResult.Failure(PasswordResetError.ServiceUnavailable), serviceUnavailableResult)
    }

    @Test
    fun validationErrorCodeMapsToDomainFailure() {
        var result: PasswordResetResult<PasswordResetEmailCode>? = null
        serveResponse(
            statusCode = 400,
            responseBody = """{"success":false,"error":{"code":"VALIDATION_ERROR"}}""",
        ) { baseUrl ->
            result = PasswordResetRepositoryImpl(remoteDataSource(baseUrl)).requestEmailCode("a@b.co")
        }

        assertEquals(PasswordResetResult.Failure(PasswordResetError.Validation), result)
    }

    private fun remoteDataSource(baseUrl: String): PasswordResetRemoteDataSourceImpl {
        val factory =
            ApiRetrofitFactory(
                authenticatedApiClient = OkHttpClient(),
                unauthenticatedApiClient = NetworkModule.provideUnauthenticatedApiClient(),
                weatherApiClient = OkHttpClient(),
                sessionManager =
                    SessionManager(
                        tokenStorage = EmptyTokenStorage,
                        ioDispatcher = Dispatchers.Unconfined,
                        sessionScopedDataCleaners = emptySet(),
                    ),
                json = NetworkModule.provideJson(),
            )
        val service =
            factory.createService(
                baseUrl = baseUrl,
                serviceClass = PasswordResetApiService::class.java,
                authentication = ApiAuthentication.NONE,
            )
        return PasswordResetRemoteDataSourceImpl(service, ApiExecutor())
    }

    private fun assertHasNoAuthenticationOrScopeHeaders(request: RecordedRequest) {
        assertFalse(request.headers.containsKey("authorization"))
        assertFalse(request.headers.containsKey("x-site-id"))
        assertFalse(request.headers.containsKey("x-team-id"))
    }

    private fun RecordedRequest.bodyJson(): JsonObject = Json.parseToJsonElement(body).jsonObject

    private fun serveResponse(
        statusCode: Int = 200,
        responseBody: String,
        action: suspend (String) -> Unit,
    ): RecordedRequest {
        val serverSocket = ServerSocket(0)
        val capturedRequest = AtomicReference<RecordedRequest?>()
        val serverFailure = AtomicReference<Throwable?>()
        val responseThread =
            Thread {
                try {
                    serverSocket.accept().use { clientSocket ->
                        clientSocket.soTimeout = SERVER_TIMEOUT_MILLISECONDS
                        val request = readRequest(clientSocket)
                        capturedRequest.set(request)
                        writeResponse(clientSocket, statusCode, responseBody)
                    }
                } catch (throwable: Throwable) {
                    serverFailure.set(throwable)
                }
            }
        responseThread.start()

        try {
            runBlocking {
                action("http://127.0.0.1:${serverSocket.localPort}/")
            }
        } finally {
            serverSocket.close()
            responseThread.join(SERVER_TIMEOUT_MILLISECONDS.toLong())
        }

        serverFailure.get()?.let { throw AssertionError("Local HTTP fixture failed", it) }
        assertFalse("Local HTTP fixture did not finish", responseThread.isAlive)
        return requireNotNull(capturedRequest.get())
    }

    private fun readRequest(clientSocket: Socket): RecordedRequest {
        val input = BufferedInputStream(clientSocket.getInputStream())
        val headerBytes = ByteArrayOutputStream()
        var endingSequence = 0
        while (endingSequence < HTTP_HEADER_ENDING.size) {
            val nextByte = input.read()
            check(nextByte != -1) { "Request ended before headers were complete" }
            headerBytes.write(nextByte)
            endingSequence =
                when {
                    nextByte == HTTP_HEADER_ENDING[endingSequence].toInt() -> endingSequence + 1
                    nextByte == HTTP_HEADER_ENDING[0].toInt() -> 1
                    else -> 0
                }
        }

        val headerLines = String(headerBytes.toByteArray(), StandardCharsets.ISO_8859_1).split("\r\n")
        val headers =
            headerLines
                .drop(1)
                .filter(String::isNotBlank)
                .associate { line ->
                    val (name, value) = line.split(":", limit = 2)
                    name.trim().lowercase() to value.trim()
                }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val bodyBytes = ByteArray(contentLength)
        var bytesRead = 0
        while (bytesRead < contentLength) {
            val readCount = input.read(bodyBytes, bytesRead, contentLength - bytesRead)
            check(readCount > 0) { "Request body ended before Content-Length" }
            bytesRead += readCount
        }

        return RecordedRequest(
            requestLine = headerLines.first(),
            headers = headers,
            body = String(bodyBytes, StandardCharsets.UTF_8),
        )
    }

    private fun writeResponse(
        clientSocket: Socket,
        statusCode: Int,
        responseBody: String,
    ) {
        val responseBytes = responseBody.toByteArray(StandardCharsets.UTF_8)
        val statusText = STATUS_TEXT[statusCode] ?: "Response"
        val responseHeaders =
            "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${responseBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        clientSocket.getOutputStream().apply {
            write(responseHeaders.toByteArray(StandardCharsets.ISO_8859_1))
            write(responseBytes)
            flush()
        }
    }

    private data class RecordedRequest(
        val requestLine: String,
        val headers: Map<String, String>,
        val body: String,
    )

    private object EmptyTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private companion object {
        val HTTP_HEADER_ENDING = byteArrayOf(13, 10, 13, 10)
        val STATUS_TEXT =
            mapOf(
                200 to "OK",
                400 to "Bad Request",
                429 to "Too Many Requests",
                503 to "Service Unavailable",
            )
        const val SERVER_TIMEOUT_MILLISECONDS = 5_000
    }
}
