package com.nativelap.heartguard.domain.notification.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import javax.inject.Inject

class MarkAllTeamNotificationsReadUseCase @Inject constructor(
    private val teamNotificationRepository: TeamNotificationRepository,
) {
    suspend operator fun invoke(): ApiResult<NotificationReadAllReceipt> =
        teamNotificationRepository.markAllNotificationsRead()
}
