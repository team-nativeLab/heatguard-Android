package com.nativelap.heartguard.data.checklist.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.checklist.remote.ChecklistApiService
import com.nativelap.heartguard.data.checklist.remote.ChecklistRemoteDataSource
import com.nativelap.heartguard.data.checklist.remote.ChecklistRemoteDataSourceImpl
import com.nativelap.heartguard.data.checklist.repository.ChecklistRepositoryImpl
import com.nativelap.heartguard.domain.checklist.repository.ChecklistRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChecklistDataModule {
    @Binds
    @Singleton
    abstract fun bindChecklistRemoteDataSource(
        impl: ChecklistRemoteDataSourceImpl,
    ): ChecklistRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        impl: ChecklistRepositoryImpl,
    ): ChecklistRepository

    companion object {
        @Provides
        @Singleton
        fun provideChecklistApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): ChecklistApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = ChecklistApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
