package com.nativelap.heartguard.data.account.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.account.repository.AccountRepositoryImpl
import com.nativelap.heartguard.data.account.remote.AccountApiService
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSourceImpl
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountDataModule {

    @Binds
    @Singleton
    abstract fun bindAccountRemoteDataSource(
        impl: AccountRemoteDataSourceImpl,
    ): AccountRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl,
    ): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideAccountApiService(
            apiRetrofitFactory: ApiRetrofitFactory,
        ): AccountApiService = apiRetrofitFactory.createService(
            baseUrl = BuildConfig.BASE_URL,
            serviceClass = AccountApiService::class.java,
            authentication = ApiAuthentication.BEARER,
        )
    }
}
