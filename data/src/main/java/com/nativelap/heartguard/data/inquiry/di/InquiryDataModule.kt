package com.nativelap.heartguard.data.inquiry.di

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.SessionScopedDataCleaner
import com.nativelap.heartguard.data.BuildConfig
import com.nativelap.heartguard.data.inquiry.local.AndroidKeystoreInquiryInputFingerprinter
import com.nativelap.heartguard.data.inquiry.local.InquiryInputFingerprinter
import com.nativelap.heartguard.data.inquiry.local.InquirySubmissionGuardLocalDataSource
import com.nativelap.heartguard.data.inquiry.local.InquirySubmissionGuardLocalDataSourceImpl
import com.nativelap.heartguard.data.inquiry.remote.InquiryApiService
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSource
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSourceImpl
import com.nativelap.heartguard.data.inquiry.repository.InquiryRepositoryImpl
import com.nativelap.heartguard.data.inquiry.repository.InquirySubmissionGuardRepositoryImpl
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import com.nativelap.heartguard.domain.inquiry.repository.InquirySubmissionGuardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InquiryDataModule {
    @Binds
    @Singleton
    abstract fun bindInquiryRemoteDataSource(implementation: InquiryRemoteDataSourceImpl): InquiryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindInquiryRepository(implementation: InquiryRepositoryImpl): InquiryRepository

    @Binds
    @Singleton
    abstract fun bindInquirySubmissionGuardLocalDataSource(
        implementation: InquirySubmissionGuardLocalDataSourceImpl,
    ): InquirySubmissionGuardLocalDataSource

    @Binds
    @Singleton
    abstract fun bindInquiryInputFingerprinter(
        implementation: AndroidKeystoreInquiryInputFingerprinter,
    ): InquiryInputFingerprinter

    @Binds
    @Singleton
    abstract fun bindInquirySubmissionGuardRepository(
        implementation: InquirySubmissionGuardRepositoryImpl,
    ): InquirySubmissionGuardRepository

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindInquirySubmissionGuardSessionCleaner(
        implementation: InquirySubmissionGuardLocalDataSourceImpl,
    ): SessionScopedDataCleaner

    companion object {
        @Provides
        @Singleton
        fun provideInquiryApiService(apiRetrofitFactory: ApiRetrofitFactory): InquiryApiService =
            apiRetrofitFactory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = InquiryApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )
    }
}
