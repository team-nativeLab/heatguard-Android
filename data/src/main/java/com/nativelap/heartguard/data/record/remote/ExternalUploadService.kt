package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.di.ExternalUploadClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** presigned URL로 사진 원본을 직접 PUT한다. S3 등 외부 저장소 주소라 앱 서버 인증(Bearer)과 무관하므로
 * 인증 헤더가 붙지 않는 [ExternalUploadClient]를 쓴다. 오류 응답은 제한된 크기에서 허용 목록의 코드만 읽는다.
 * 코루틴이 취소되면(화면 이탈·세션 종료) 진행 중인 업로드 요청도 함께 취소한다. */
class ExternalUploadService
    @Inject
    constructor(
        @param:ExternalUploadClient private val externalUploadClient: OkHttpClient,
    ) {
        /** [uploadUrl]에 [photoContent]를 PUT하고 HTTP 상태와 안전하게 추출한 오류 코드를 돌려준다.
         * 연결 실패는 IOException으로 던진다.
         * 서버가 준 [requiredHeaders](예: Content-Type)를 그대로 붙여야 서명 검증을 통과한다. */
        internal suspend fun uploadFile(
            uploadUrl: String,
            requiredHeaders: Map<String, String>,
            photoContent: RequestBody,
        ): ExternalUploadResult {
            val requestBuilder =
                Request
                    .Builder()
                    .url(uploadUrl)
                    .put(photoContent)
            requiredHeaders.forEach { (headerName, headerValue) ->
                requestBuilder.header(headerName, headerValue)
            }
            val uploadClient =
                externalUploadClient
                    .newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()
            val uploadCall = uploadClient.newCall(requestBuilder.build())

            return suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    uploadCall.cancel()
                }
                uploadCall.enqueue(
                    object : Callback {
                        override fun onFailure(
                            call: Call,
                            e: IOException,
                        ) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(e)
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response,
                        ) {
                            val uploadResult =
                                response.use { uploadResponse ->
                                    val errorCode =
                                        try {
                                            uploadResponse.readS3UploadErrorCode()
                                        } catch (_: IOException) {
                                            null
                                        }
                                    ExternalUploadResult(
                                        statusCode = uploadResponse.code,
                                        errorCode = errorCode,
                                    )
                                }
                            continuation.resume(uploadResult)
                        }
                    },
                )
            }
        }

        private fun Response.readS3UploadErrorCode(): String? {
            if (isSuccessful) {
                return null
            }

            val responseBody = body ?: return null
            val responseSource = responseBody.source()
            val errorBuffer = Buffer()
            var errorBodyByteCount = 0L

            while (errorBodyByteCount <= MAX_ERROR_BODY_BYTES) {
                val bytesToRead = minOf(ERROR_BODY_READ_BUFFER_BYTES, MAX_ERROR_BODY_BYTES + 1 - errorBodyByteCount)
                val bytesRead = responseSource.read(errorBuffer, bytesToRead)
                if (bytesRead < 0) {
                    break
                }

                errorBodyByteCount += bytesRead
            }

            if (errorBodyByteCount > MAX_ERROR_BODY_BYTES) {
                return null
            }

            return S3UploadErrorCodeReader.read(errorBuffer.readUtf8())
        }

        private companion object {
            const val MAX_ERROR_BODY_BYTES = 16L * 1024L
            const val ERROR_BODY_READ_BUFFER_BYTES = 4L * 1024L
        }
    }
