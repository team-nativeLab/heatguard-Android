package com.nativelap.heartguard.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** 서버 공통 오류 응답({"success":false,"error":{"code":"...","message":"..."}})에서 오류 코드만 꺼낸다.
 * ApiExecutor와 BearerTokenAuthenticator가 함께 쓰며, Authenticator가 SessionManager 외의 의존성을 주입받지 않도록
 * DI 대상이 아닌 순수 객체로 둔다. JSON이 아니거나 형식이 다르면 예외 없이 null을 돌려준다. */
internal object ApiErrorCodeReader {
    // 오류 본문은 짧은 JSON이므로 이 크기를 넘는 본문은 읽지 않는다. 잘린 JSON은 파싱에 실패해 null이 된다.
    const val MAX_ERROR_BODY_BYTES = 16L * 1024L

    private val errorJson = Json {
        ignoreUnknownKeys = true
    }

    fun read(errorBody: String): String? {
        if (errorBody.isBlank()) {
            return null
        }

        return try {
            errorJson.decodeFromString<ApiErrorBody>(errorBody).error?.code
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    @Serializable
    private data class ApiErrorBody(
        val error: ApiEnvelopeError? = null,
    )
}
