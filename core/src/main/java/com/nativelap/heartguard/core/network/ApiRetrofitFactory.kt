package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.network.di.AuthenticatedApiClient
import com.nativelap.heartguard.core.network.di.UnauthenticatedApiClient
import com.nativelap.heartguard.core.network.di.WeatherApiClient
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject

/** 실제 서버 주소가 확정된 뒤 인증 정책별 Retrofit API를 생성한다. */
class ApiRetrofitFactory
    @Inject
    constructor(
        @param:AuthenticatedApiClient private val authenticatedApiClient: OkHttpClient,
        @param:UnauthenticatedApiClient private val unauthenticatedApiClient: OkHttpClient,
        @param:WeatherApiClient private val weatherApiClient: OkHttpClient,
        private val sessionManager: SessionManager,
        private val json: Json,
    ) {
        private val singleAttemptClient by lazy {
            authenticatedApiClient
                .newBuilder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val originalBody = originalRequest.body
                    val singleAttemptRequest =
                        if (originalBody != null) {
                            originalRequest
                                .newBuilder()
                                .method(originalRequest.method, SingleAttemptRequestBody(originalBody))
                                .build()
                        } else {
                            originalRequest
                        }
                    chain.proceed(singleAttemptRequest)
                }.build()
        }

        fun <ApiService : Any> createService(
            baseUrl: String,
            serviceClass: Class<ApiService>,
            authentication: ApiAuthentication,
            allowRequestReplay: Boolean = true,
            useWeatherTimeout: Boolean = false,
        ): ApiService {
            val callFactory: Call.Factory =
                when (authentication) {
                    ApiAuthentication.NONE -> {
                        unauthenticatedApiClient
                    }

                    ApiAuthentication.BEARER,
                    ApiAuthentication.APP_ACCOUNT_BEARER,
                    -> {
                        SessionBoundCallFactory(
                            delegateClient =
                                if (allowRequestReplay) {
                                    if (useWeatherTimeout) weatherApiClient else authenticatedApiClient
                                } else {
                                    singleAttemptClient
                                },
                            sessionManager = sessionManager,
                            requiredRole =
                                if (authentication == ApiAuthentication.APP_ACCOUNT_BEARER) {
                                    SessionRole.APP_ACCOUNT
                                } else {
                                    SessionRole.TEAM_MEMBER
                                },
                        )
                    }

                    ApiAuthentication.APP_ACCOUNT_SITE_BEARER -> {
                        val authenticatedClient =
                            if (allowRequestReplay) {
                                authenticatedApiClient
                            } else {
                                singleAttemptClient
                            }
                        val siteScopedClient =
                            authenticatedClient
                                .newBuilder()
                                .addInterceptor(SelectedSiteIdInterceptor(sessionManager))
                                .build()

                        SessionBoundCallFactory(
                            delegateClient = siteScopedClient,
                            sessionManager = sessionManager,
                            requiredRole = SessionRole.APP_ACCOUNT,
                        )
                    }

                    ApiAuthentication.TEAM_SITE_BEARER -> {
                        val authenticatedClient =
                            if (allowRequestReplay) {
                                authenticatedApiClient
                            } else {
                                singleAttemptClient
                            }
                        val siteScopedClient =
                            authenticatedClient
                                .newBuilder()
                                .addInterceptor(SelectedSiteIdInterceptor(sessionManager))
                                .build()

                        SessionBoundCallFactory(
                            delegateClient = siteScopedClient,
                            sessionManager = sessionManager,
                            requiredRole = SessionRole.TEAM_MEMBER,
                        )
                    }

                    ApiAuthentication.TEAM_CHECKLIST_BEARER -> {
                        val authenticatedClient =
                            if (allowRequestReplay) authenticatedApiClient else singleAttemptClient
                        val teamScopedClient =
                            authenticatedClient
                                .newBuilder()
                                .addInterceptor(SelectedTeamIdInterceptor(sessionManager))
                                .build()
                        SessionBoundCallFactory(
                            delegateClient = teamScopedClient,
                            sessionManager = sessionManager,
                            requiredRole = SessionRole.TEAM_MEMBER,
                            captureSelectedTeam = true,
                        )
                    }
                }
            val contentType = JSON_MEDIA_TYPE.toMediaType()

            val parsedBaseUrl = baseUrl.toHttpUrl()
            require(parsedBaseUrl.encodedPath.endsWith('/')) {
                "Retrofit base URL must end with a slash."
            }

            return Retrofit
                .Builder()
                .baseUrl(parsedBaseUrl)
                .callFactory(callFactory)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(serviceClass)
        }

        private companion object {
            const val JSON_MEDIA_TYPE = "application/json; charset=UTF-8"
        }
    }
