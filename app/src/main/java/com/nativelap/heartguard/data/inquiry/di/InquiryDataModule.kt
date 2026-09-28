package com.nativelap.heartguard.data.inquiry.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.inquiry.remote.InquiryApiService
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSourceImpl
import com.nativelap.heartguard.data.inquiry.repository.InquiryRepositoryImpl
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InquiryDataModule {
    @Binds
    @Singleton
    abstract fun bindInquiryRemoteDataSource(
        impl: InquiryRemoteDataSourceImpl,
    ): InquiryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindInquiryRepository(
        impl: InquiryRepositoryImpl,
    ): InquiryRepository

    companion object {
        @Provides
        @Singleton
        fun provideInquiryApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): InquiryApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = InquiryApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
