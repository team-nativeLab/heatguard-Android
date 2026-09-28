package com.nativelap.heartguard.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    // API 명세의 날짜·체크시간 기준(Asia/Seoul)에 맞춘 시계다. 테스트에서는 고정 Clock으로 바꿔 끼운다.
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.system(ZoneId.of("Asia/Seoul"))
}
