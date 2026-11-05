package com.nativelap.heartguard.core.util

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfilePhotoMemoryCacheModule {
    @Binds
    @Singleton
    abstract fun bindProfilePhotoMemoryCache(implementation: CoilProfilePhotoMemoryCache): ProfilePhotoMemoryCache
}
