package com.nativelap.heartguard.core.di

import com.nativelap.heartguard.core.session.AndroidKeystoreTokenStorage
import com.nativelap.heartguard.core.session.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionStorageModule {
    @Binds
    @Singleton
    abstract fun bindTokenStorage(
        tokenStorage: AndroidKeystoreTokenStorage,
    ): TokenStorage
}
