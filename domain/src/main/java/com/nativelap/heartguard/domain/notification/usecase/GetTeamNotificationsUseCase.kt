package com.nativelap.heartguard.domain.notification.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import javax.inject.Inject

class GetTeamNotificationsUseCase
    @Inject
    constructor(
        private val teamNotificationRepository: TeamNotificationRepository,
    ) {
        suspend operator fun invoke(
            category: NotificationCategory,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPage> =
            teamNotificationRepository.getNotifications(
                category = category,
                cursor = cursor,
                limit = limit,
            )
    }
