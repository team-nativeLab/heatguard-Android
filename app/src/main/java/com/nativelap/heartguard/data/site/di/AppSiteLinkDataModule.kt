package com.nativelap.heartguard.data.site.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.site.remote.AppSiteDashboardApiService
import com.nativelap.heartguard.data.site.remote.AppSiteLinkApiService
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSource
import com.nativelap.heartguard.data.site.remote.AppSiteLinkRemoteDataSourceImpl
import com.nativelap.heartguard.data.site.repository.AppSiteLinkRepositoryImpl
import com.nativelap.heartguard.domain.site.repository.AppSiteLinkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppSiteLinkDataModule {
    @Binds
    @Singleton
    abstract fun bindAppSiteLinkRemoteDataSource(impl: AppSiteLinkRemoteDataSourceImpl): AppSiteLinkRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAppSiteLinkRepository(impl: AppSiteLinkRepositoryImpl): AppSiteLinkRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppSiteLinkApiService(apiRetrofitFactory: ApiRetrofitFactory): AppSiteLinkApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = AppSiteLinkApiService::class.java,
                authentication = ApiAuthentication.BEARER,
                allowRequestReplay = false,
            )

        @Provides
        @Singleton
        fun provideAppSiteDashboardApiService(apiRetrofitFactory: ApiRetrofitFactory): AppSiteDashboardApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = AppSiteDashboardApiService::class.java,
                authentication = ApiAuthentication.TEAM_SITE_BEARER,
                allowRequestReplay = false,
            )
    }
}
