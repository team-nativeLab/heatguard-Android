package com.nativelap.heartguard.data.profile.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.profile.remote.WorkerProfileApiService
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSource
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSourceImpl
import com.nativelap.heartguard.data.profile.repository.WorkerProfileRepositoryImpl
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerProfileDataModule {
    @Binds
    @Singleton
    abstract fun bindWorkerProfileRemoteDataSource(
        implementation: WorkerProfileRemoteDataSourceImpl,
    ): WorkerProfileRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindWorkerProfileRepository(
        implementation: WorkerProfileRepositoryImpl,
    ): WorkerProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideWorkerProfileApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): WorkerProfileApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = WorkerProfileApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
