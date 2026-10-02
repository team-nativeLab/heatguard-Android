package com.nativelap.heartguard.domain.notification.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import javax.inject.Inject

class MarkTeamNotificationReadUseCase @Inject constructor(
    private val teamNotificationRepository: TeamNotificationRepository,
) {
    suspend operator fun invoke(notificationId: String): ApiResult<NotificationReadReceipt> =
        teamNotificationRepository.markNotificationRead(notificationId)
}
