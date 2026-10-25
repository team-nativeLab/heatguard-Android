package com.nativelap.heartguard.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedApiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedApiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExternalUploadClient
