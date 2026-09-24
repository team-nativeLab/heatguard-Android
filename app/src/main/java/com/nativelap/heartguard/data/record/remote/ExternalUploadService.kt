package com.nativelap.heartguard.data.record.remote

import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.di.ExternalUploadClient
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** presigned URL로 사진 원본을 직접 업로드한다. S3 등 외부 저장소 주소이며 앱 서버 인증(Bearer)과
 * 무관하므로, 인증 헤더가 붙지 않는 [ExternalUploadClient] OkHttpClient를 그대로 쓴다. Retrofit의
 * kotlinx.serialization 컨버터를 쓰지 않는 이유는 이 응답이 JSON이 아니라 보통 빈 body이기 때문이다. */
class ExternalUploadService @Inject constructor(
    @param:ExternalUploadClient private val externalUploadClient: OkHttpClient,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun uploadFile(
        uploadUrl: String,
        requiredHeaders: Map<String, String>,
        contentType: String,
        bytes: ByteArray,
    ) {
        withContext(ioDispatcher) {
            val requestBody = bytes.toRequestBody(contentType.toMediaType())
            val requestBuilder = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
            requiredHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }

            externalUploadClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("사진 업로드에 실패했습니다: HTTP ${response.code}")
                }
            }
        }
    }
}
