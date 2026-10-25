package com.nativelap.heartguard.data.notification.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.requireSuccessData
import com.nativelap.heartguard.data.notification.dto.NotificationReadAllReceiptDto
import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import javax.inject.Inject

class TeamNotificationRemoteDataSourceImpl
    @Inject
    constructor(
        private val teamNotificationApiService: TeamNotificationApiService,
        private val apiExecutor: ApiExecutor,
    ) : TeamNotificationRemoteDataSource {
        override suspend fun getNotifications(
            category: String,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPageDto> =
            apiExecutor
                .execute {
                    teamNotificationApiService.getNotifications(
                        category = category,
                        limit = limit,
                        cursor = cursor,
                    )
                }.requireSuccessData()

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceiptDto> =
            apiExecutor
                .execute {
                    teamNotificationApiService.markNotificationRead(notificationId)
                }.requireSuccessData()

        override suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceiptDto> =
            apiExecutor
                .execute {
                    teamNotificationApiService.markAllNotificationsRead()
                }.requireSuccessData()
    }
