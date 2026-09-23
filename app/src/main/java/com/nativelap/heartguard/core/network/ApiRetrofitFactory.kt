package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.network.di.AuthenticatedApiClient
import com.nativelap.heartguard.core.network.di.UnauthenticatedApiClient
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

/** 실제 서버 주소가 확정된 뒤 인증 정책별 Retrofit API를 생성한다. */
class ApiRetrofitFactory @Inject constructor(
    @param:AuthenticatedApiClient private val authenticatedApiClient: OkHttpClient,
    @param:UnauthenticatedApiClient private val unauthenticatedApiClient: OkHttpClient,
    private val json: Json,
) {
    fun <ApiService : Any> createService(
        baseUrl: String,
        serviceClass: Class<ApiService>,
        authentication: ApiAuthentication,
    ): ApiService {
        val client = when (authentication) {
            ApiAuthentication.NONE -> unauthenticatedApiClient
            ApiAuthentication.BEARER -> authenticatedApiClient
        }
        val contentType = JSON_MEDIA_TYPE.toMediaType()

        val parsedBaseUrl = baseUrl.toHttpUrl()
        require(parsedBaseUrl.encodedPath.endsWith('/')) {
            "Retrofit base URL must end with a slash."
        }

        return Retrofit.Builder()
            .baseUrl(parsedBaseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(serviceClass)
    }

    private companion object {
        const val JSON_MEDIA_TYPE = "application/json; charset=UTF-8"
    }
}
