package com.nativelap.heartguard.data.auth.remote

import retrofit2.http.POST

interface TeamSessionApiService {
    @POST("api/v1/auth/team/logout")
    suspend fun logout()
}
