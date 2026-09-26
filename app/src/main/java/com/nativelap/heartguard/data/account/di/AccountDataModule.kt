package com.nativelap.heartguard.data.account.di

import com.nativelap.heartguard.BuildConfig
import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.account.remote.AccountApiService
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSourceImpl
import com.nativelap.heartguard.data.account.repository.AccountRepositoryImpl
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
        // 회원탈퇴는 로그인 세션이 필요한 API라 Bearer 토큰을 붙이는 인증 클라이언트로 만든다.
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
