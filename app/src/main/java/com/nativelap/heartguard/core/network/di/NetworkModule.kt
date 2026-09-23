package com.nativelap.heartguard.core.network.di

import com.nativelap.heartguard.core.network.BearerTokenInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    @UnauthenticatedApiClient
    fun provideUnauthenticatedApiClient(): OkHttpClient = OkHttpClient.Builder()
        .build()

    @Provides
    @Singleton
    @AuthenticatedApiClient
    fun provideAuthenticatedApiClient(
        bearerTokenInterceptor: BearerTokenInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(bearerTokenInterceptor)
        .build()

    @Provides
    @Singleton
    @ExternalUploadClient
    fun provideExternalUploadClient(): OkHttpClient = OkHttpClient.Builder()
        .build()

}
