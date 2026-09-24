package com.nativelap.heartguard.core.di

import com.nativelap.heartguard.core.session.DevTeamTokenProvider
import com.nativelap.heartguard.core.session.TeamTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TeamTokenModule {
    @Binds
    @Singleton
    abstract fun bindTeamTokenProvider(
        devTeamTokenProvider: DevTeamTokenProvider,
    ): TeamTokenProvider
}
