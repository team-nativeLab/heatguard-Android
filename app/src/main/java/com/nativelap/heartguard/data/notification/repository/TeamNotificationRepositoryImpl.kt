package com.nativelap.heartguard.data.notification.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.notification.mapper.toDomain
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSource
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import javax.inject.Inject

class TeamNotificationRepositoryImpl @Inject constructor(
    private val teamNotificationRemoteDataSource: TeamNotificationRemoteDataSource,
) : TeamNotificationRepository {
    override suspend fun getNotifications(
        category: NotificationCategory,
        cursor: String?,
        limit: Int,
    ): ApiResult<TeamNotificationPage> {
        val categoryParameter = requireNotNull(category.apiValue) {
            "UNKNOWN 분류는 서버 필터로 요청할 수 없습니다."
        }
        return teamNotificationRemoteDataSource.getNotifications(
            category = categoryParameter,
            cursor = cursor,
            limit = limit,
        ).map { notificationPage -> notificationPage.toDomain() }
    }

    override suspend fun markNotificationRead(
        notificationId: String,
    ): ApiResult<NotificationReadReceipt> = teamNotificationRemoteDataSource
        .markNotificationRead(notificationId)
        .map { readReceipt -> readReceipt.toDomain() }
}
