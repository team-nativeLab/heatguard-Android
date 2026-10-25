package com.nativelap.heartguard.data.notification.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.notification.dto.NotificationReadAllReceiptDto
import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface TeamNotificationApiService {
    @GET("api/v1/team/notifications")
    suspend fun getNotifications(
        @Query("category") category: String,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): ApiEnvelope<TeamNotificationPageDto>

    @PATCH("api/v1/team/notifications/{notificationId}/read")
    suspend fun markNotificationRead(
        @Path("notificationId") notificationId: String,
    ): ApiEnvelope<NotificationReadReceiptDto>

    @PATCH("api/v1/team/notifications/read-all")
    suspend fun markAllNotificationsRead(): ApiEnvelope<NotificationReadAllReceiptDto>
}
