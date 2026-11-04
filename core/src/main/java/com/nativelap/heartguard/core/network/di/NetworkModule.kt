package com.nativelap.heartguard.core.network.di

import com.nativelap.heartguard.core.network.BearerTokenAuthenticator
import com.nativelap.heartguard.core.network.BearerTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
        }

    @Provides
    @Singleton
    @UnauthenticatedApiClient
    fun provideUnauthenticatedApiClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .applyApiTimeouts()
            .build()

    @Provides
    @Singleton
    @AuthenticatedApiClient
    fun provideAuthenticatedApiClient(
        bearerTokenInterceptor: BearerTokenInterceptor,
        bearerTokenAuthenticator: BearerTokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .applyApiTimeouts()
            .addInterceptor(bearerTokenInterceptor)
            .authenticator(bearerTokenAuthenticator)
            .build()

    @Provides
    @Singleton
    @ExternalUploadClient
    fun provideExternalUploadClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .build()

    // 앱 서버 요청이 응답 없이 오래 매달리지 않도록 단계별 시간과 요청 전체 시간을 함께 제한한다.
    private fun OkHttpClient.Builder.applyApiTimeouts(): OkHttpClient.Builder =
        connectTimeout(API_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(API_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(API_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(API_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private const val API_CONNECT_TIMEOUT_SECONDS = 10L
    private const val API_READ_TIMEOUT_SECONDS = 10L
    private const val API_WRITE_TIMEOUT_SECONDS = 10L
    private const val API_CALL_TIMEOUT_SECONDS = 20L
}
