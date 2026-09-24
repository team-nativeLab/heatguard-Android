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
    abstract fun bindTeamSiteRemoteDataSource(
        impl: TeamSiteRemoteDataSourceImpl,
    ): TeamSiteRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTeamSiteRepository(
        impl: TeamSiteRepositoryImpl,
    ): TeamSiteRepository

    companion object {
        // TEAM_TOKEN은 URL Path 파라미터로 전달되고 API 문서상 별도 Authorization 헤더 요구가
        // 없으므로 인증 정책 없이(NONE) Retrofit 서비스를 만든다.
        @Provides
        @Singleton
        fun provideTeamSiteApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): TeamSiteApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = TeamSiteApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )
    }
}
