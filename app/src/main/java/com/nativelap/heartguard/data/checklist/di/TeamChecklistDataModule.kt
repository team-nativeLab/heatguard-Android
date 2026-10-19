package com.nativelap.heartguard.data.checklist.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistApiService
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSource
import com.nativelap.heartguard.data.checklist.remote.TeamChecklistRemoteDataSourceImpl
import com.nativelap.heartguard.data.checklist.repository.TeamChecklistRepositoryImpl
import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamChecklistDataModule {
    @Binds
    @Singleton
    abstract fun bindTeamChecklistRemoteDataSource(
        implementation: TeamChecklistRemoteDataSourceImpl,
    ): TeamChecklistRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTeamChecklistRepository(implementation: TeamChecklistRepositoryImpl): TeamChecklistRepository

    companion object {
        @Provides
        @Singleton
        fun provideTeamChecklistApiService(apiRetrofitFactory: ApiRetrofitFactory): TeamChecklistApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = TeamChecklistApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )
    }
}
