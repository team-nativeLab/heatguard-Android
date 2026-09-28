package com.nativelap.heartguard.data.auth.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.auth.remote.TeamAuthRemoteDataSource
import com.nativelap.heartguard.data.auth.remote.TeamAuthRemoteDataSourceImpl
import com.nativelap.heartguard.data.auth.remote.TeamLoginApiService
import com.nativelap.heartguard.data.auth.remote.TeamSessionApiService
import com.nativelap.heartguard.data.auth.repository.TeamAuthRepositoryImpl
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamAuthDataModule {
    @Binds
    @Singleton
    abstract fun bindTeamAuthRemoteDataSource(
        impl: TeamAuthRemoteDataSourceImpl,
    ): TeamAuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTeamAuthRepository(
        impl: TeamAuthRepositoryImpl,
    ): TeamAuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideTeamLoginApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): TeamLoginApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = TeamLoginApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )

        @Provides
        @Singleton
        fun provideTeamSessionApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): TeamSessionApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = TeamSessionApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
