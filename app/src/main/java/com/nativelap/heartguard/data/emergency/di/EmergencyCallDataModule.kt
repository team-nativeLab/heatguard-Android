package com.nativelap.heartguard.data.emergency.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallApiService
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSource
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSourceImpl
import com.nativelap.heartguard.data.emergency.repository.EmergencyCallRepositoryImpl
import com.nativelap.heartguard.domain.emergency.repository.EmergencyCallRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmergencyCallDataModule {

    @Binds
    @Singleton
    abstract fun bindEmergencyCallRemoteDataSource(
        impl: EmergencyCallRemoteDataSourceImpl,
    ): EmergencyCallRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindEmergencyCallRepository(
        impl: EmergencyCallRepositoryImpl,
    ): EmergencyCallRepository

    companion object {
        @Provides
        @Singleton
        fun provideEmergencyCallApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): EmergencyCallApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = EmergencyCallApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )
    }
}
