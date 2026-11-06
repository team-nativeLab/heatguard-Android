package com.nativelap.heartguard.core.location

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CurrentLocationModule {
    @Binds
    @Singleton
    abstract fun bindCurrentLocationProvider(
        implementation: AndroidCurrentLocationProvider,
    ): CurrentLocationProvider
}
