package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.network.di.ExternalUploadClient
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

/** presigned URL로 사진 원본을 직접 PUT한다. S3 등 외부 저장소 주소라 앱 서버 인증(Bearer)과 무관하므로
 * 인증 헤더가 붙지 않는 [ExternalUploadClient]를 쓴다. 응답 본문은 보통 비어 있어 HTTP 상태 코드만 돌려준다.
 * 코루틴이 취소되면(화면 이탈·세션 종료) 진행 중인 업로드 요청도 함께 취소한다. */
class ExternalUploadService @Inject constructor(
    @param:ExternalUploadClient private val externalUploadClient: OkHttpClient,
) {
    /** [uploadUrl]에 [photoContent]를 PUT하고 HTTP 상태 코드를 돌려준다. 연결 실패는 IOException으로 던진다.
     * 서버가 준 [requiredHeaders](예: Content-Type)를 그대로 붙여야 서명 검증을 통과한다. */
    suspend fun uploadFile(
        uploadUrl: String,
        requiredHeaders: Map<String, String>,
        photoContent: RequestBody,
    ): Int {
        val requestBuilder = Request.Builder()
            .url(uploadUrl)
            .put(photoContent)
        requiredHeaders.forEach { (headerName, headerValue) ->
            requestBuilder.header(headerName, headerValue)
        }
        val uploadCall = externalUploadClient.newCall(requestBuilder.build())

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                uploadCall.cancel()
            }
            uploadCall.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val statusCode = response.use { uploadResponse -> uploadResponse.code }
                        continuation.resume(statusCode)
                    }
                },
            )
        }
    }
}
