package com.nativelap.heartguard.data.record.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.record.remote.RecordApiService
import com.nativelap.heartguard.data.record.remote.RecordRemoteDataSource
import com.nativelap.heartguard.data.record.remote.RecordRemoteDataSourceImpl
import com.nativelap.heartguard.data.record.remote.UploadApiService
import com.nativelap.heartguard.data.record.repository.RecordRepositoryImpl
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordDataModule {

    @Binds
    @Singleton
    abstract fun bindRecordRemoteDataSource(
        impl: RecordRemoteDataSourceImpl,
    ): RecordRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRecordRepository(
        impl: RecordRepositoryImpl,
    ): RecordRepository

    companion object {
        @Provides
        @Singleton
        fun provideUploadApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): UploadApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = UploadApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )

        @Provides
        @Singleton
        fun provideRecordApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): RecordApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = RecordApiService::class.java,
            authentication = ApiAuthentication.NONE,
        )
    }
}
