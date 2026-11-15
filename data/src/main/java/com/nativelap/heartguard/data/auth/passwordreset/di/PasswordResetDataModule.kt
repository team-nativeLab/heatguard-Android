package com.nativelap.heartguard.data.auth.passwordreset.di

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.BuildConfig
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetApiService
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetRemoteDataSource
import com.nativelap.heartguard.data.auth.passwordreset.remote.PasswordResetRemoteDataSourceImpl
import com.nativelap.heartguard.data.auth.passwordreset.repository.PasswordResetRepositoryImpl
import com.nativelap.heartguard.domain.auth.passwordreset.repository.PasswordResetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PasswordResetDataModule {
    @Binds @Singleton
    abstract fun bindRemote(impl: PasswordResetRemoteDataSourceImpl): PasswordResetRemoteDataSource

    @Binds @Singleton
    abstract fun bindRepository(impl: PasswordResetRepositoryImpl): PasswordResetRepository

    companion object {
        @Provides @Singleton
        fun provideService(factory: ApiRetrofitFactory): PasswordResetApiService =
            factory.createService(
                baseUrl = BuildConfig.BASE_URL,
                serviceClass = PasswordResetApiService::class.java,
                authentication = ApiAuthentication.NONE,
            )
    }
}
