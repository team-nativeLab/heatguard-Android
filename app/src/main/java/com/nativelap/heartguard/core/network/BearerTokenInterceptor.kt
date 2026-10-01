package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionSnapshot
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** 로그인 외 인증 API에 요청을 만든 세션의 accessToken을 Bearer 헤더로 전달한다.
 * 대기열에 있는 동안 세션이 끝나거나 바뀌었으면 이전 계정 요청이 새 계정 토큰으로 나가지 않도록 전송을 중단한다. */
class BearerTokenInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentSnapshot = sessionManager.getSnapshot()
        val requestSnapshot = originalRequest.tag(SessionSnapshot::class.java) ?: currentSnapshot
        if (requestSnapshot != currentSnapshot) {
            throw SessionChangedException()
        }
        val accessToken = requestSnapshot.accessToken
            ?.takeIf(String::isNotBlank)
            ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
            .tag(SessionSnapshot::class.java, requestSnapshot)
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
