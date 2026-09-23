package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.TokenStorage
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** 로그인 외 인증 API에 저장된 accessToken을 Bearer 헤더로 전달한다. */
class BearerTokenInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = tokenStorage.readAccessToken()
            ?.takeIf(String::isNotBlank)
            ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
