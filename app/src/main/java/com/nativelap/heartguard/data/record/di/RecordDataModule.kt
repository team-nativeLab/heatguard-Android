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
import com.nativelap.heartguard.data.record.remote.RecordHistoryApiService
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSource
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSourceImpl
import com.nativelap.heartguard.data.record.repository.RecordHistoryRepositoryImpl
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
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
    abstract fun bindRecordHistoryRemoteDataSource(
        impl: RecordHistoryRemoteDataSourceImpl,
    ): RecordHistoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRecordHistoryRepository(
        impl: RecordHistoryRepositoryImpl,
    ): RecordHistoryRepository

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
            authentication = ApiAuthentication.BEARER,
        )

        @Provides
        @Singleton
        fun provideRecordHistoryApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): RecordHistoryApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = RecordHistoryApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )

        @Provides
        @Singleton
        fun provideRecordApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): RecordApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = RecordApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
