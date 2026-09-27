package com.nativelap.heartguard.data.account.di

import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSource
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSourceImpl
import com.nativelap.heartguard.data.account.repository.AccountRepositoryImpl
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// TODO: 계정 삭제 API 명세가 확정되면 EmergencyCallDataModule처럼 AccountApiService의 @Provides를 추가한다.
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
}
