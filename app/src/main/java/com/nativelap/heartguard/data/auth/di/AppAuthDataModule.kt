package com.nativelap.heartguard.data.auth.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.auth.remote.AppAccountSessionApiService
import com.nativelap.heartguard.data.auth.remote.AppAuthApiService
import com.nativelap.heartguard.data.auth.remote.AppAuthRemoteDataSource
import com.nativelap.heartguard.data.auth.remote.AppAuthRemoteDataSourceImpl
import com.nativelap.heartguard.data.auth.repository.AppAuthRepositoryImpl
import com.nativelap.heartguard.domain.auth.repository.AppAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppAuthDataModule {
    @Binds
    @Singleton
    abstract fun bindAppAuthRemoteDataSource(impl: AppAuthRemoteDataSourceImpl): AppAuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAppAuthRepository(impl: AppAuthRepositoryImpl): AppAuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppAuthApiService(apiRetrofitFactory: ApiRetrofitFactory): AppAuthApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = AppAuthApiService::class.java,
                authentication = ApiAuthentication.NONE,
            )

        @Provides
        @Singleton
        fun provideAppAccountSessionApiService(apiRetrofitFactory: ApiRetrofitFactory): AppAccountSessionApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = AppAccountSessionApiService::class.java,
                authentication = ApiAuthentication.APP_ACCOUNT_BEARER,
                allowRequestReplay = false,
            )
    }
}
