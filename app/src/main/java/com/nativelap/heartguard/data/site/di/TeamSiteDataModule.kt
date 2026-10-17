package com.nativelap.heartguard.data.site.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.site.remote.TeamSiteApiService
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSource
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSourceImpl
import com.nativelap.heartguard.data.site.repository.TeamSiteRepositoryImpl
import com.nativelap.heartguard.domain.site.repository.TeamSiteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamSiteDataModule {
    @Binds
    @Singleton
    abstract fun bindTeamSiteRemoteDataSource(impl: TeamSiteRemoteDataSourceImpl): TeamSiteRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTeamSiteRepository(impl: TeamSiteRepositoryImpl): TeamSiteRepository

    companion object {
        // GET /api/v1/team은 작업자 Bearer 세션을 사용한다.
        @Provides
        @Singleton
        fun provideTeamSiteApiService(apiRetrofitFactory: ApiRetrofitFactory): TeamSiteApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = TeamSiteApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )
    }
}
