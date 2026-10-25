package com.nativelap.heartguard.data.notification.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.notification.remote.TeamNotificationApiService
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSource
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSourceImpl
import com.nativelap.heartguard.data.notification.repository.TeamNotificationRepositoryImpl
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamNotificationDataModule {
    @Binds
    @Singleton
    abstract fun bindTeamNotificationRemoteDataSource(
        implementation: TeamNotificationRemoteDataSourceImpl,
    ): TeamNotificationRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTeamNotificationRepository(
        implementation: TeamNotificationRepositoryImpl,
    ): TeamNotificationRepository

    companion object {
        @Provides
        @Singleton
        fun provideTeamNotificationApiService(apiRetrofitFactory: ApiRetrofitFactory): TeamNotificationApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = TeamNotificationApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )
    }
}
