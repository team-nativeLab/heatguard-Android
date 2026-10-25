package com.nativelap.heartguard.data.notification.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
import com.nativelap.heartguard.data.notification.mapper.toDomain
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSource
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import javax.inject.Inject

class TeamNotificationRepositoryImpl
    @Inject
    constructor(
        private val teamNotificationRemoteDataSource: TeamNotificationRemoteDataSource,
    ) : TeamNotificationRepository {
        override suspend fun getNotifications(
            category: NotificationCategory,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPage> {
            val categoryParameter =
                requireNotNull(category.apiValue) {
                    "UNKNOWN 분류는 서버 필터로 요청할 수 없습니다."
                }
            val pageResult =
                teamNotificationRemoteDataSource.getNotifications(
                    category = categoryParameter,
                    cursor = cursor,
                    limit = limit,
                )
            val notificationPage =
                when (pageResult) {
                    is ApiResult.Success -> pageResult.value
                    is ApiResult.Failure -> return pageResult
                }
            // 명세상 필수인 page 정보가 없거나, 더 있다는데 다음 커서가 없으면 목록이 잘린 것이므로 실패로 바꾼다.
            val pageInfo = notificationPage.page ?: return ApiResult.Failure(ApiError.Unknown)
            if (pageInfo.hasMore && pageInfo.nextCursor.isNullOrBlank()) {
                return ApiResult.Failure(ApiError.Unknown)
            }
            return ApiResult.Success(notificationPage.toDomain())
        }

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceipt> =
            teamNotificationRemoteDataSource
                .markNotificationRead(notificationId)
                .map { readReceipt -> readReceipt.toDomain() }

        override suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceipt> =
            teamNotificationRemoteDataSource
                .markAllNotificationsRead()
                .map { readAllReceipt -> readAllReceipt.toDomain() }
    }
