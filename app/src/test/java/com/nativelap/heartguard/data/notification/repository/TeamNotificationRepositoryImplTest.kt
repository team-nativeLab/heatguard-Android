package com.nativelap.heartguard.data.notification.repository

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.notification.dto.NotificationReadAllReceiptDto
import com.nativelap.heartguard.data.notification.dto.NotificationReadReceiptDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationCursorDto
import com.nativelap.heartguard.data.notification.dto.TeamNotificationPageDto
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSource
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TeamNotificationRepositoryImplTest {
    @Test
    fun pageWithMoreItemsButNoCursorIsFailure() =
        runTest {
            listOf(null, "", " ").forEach { missingCursor ->
                val repository =
                    TeamNotificationRepositoryImpl(
                        FakeNotificationSource(
                            TeamNotificationPageDto(
                                page = TeamNotificationCursorDto(nextCursor = missingCursor, hasMore = true),
                            ),
                        ),
                    )

                assertEquals(
                    ApiResult.Failure(ApiError.Unknown),
                    repository.getNotifications(NotificationCategory.ALL, cursor = null, limit = 20),
                )
            }
        }

    @Test
    fun pageWithoutPaginationInfoIsFailure() =
        runTest {
            val repository = TeamNotificationRepositoryImpl(FakeNotificationSource(TeamNotificationPageDto()))

            assertEquals(
                ApiResult.Failure(ApiError.Unknown),
                repository.getNotifications(NotificationCategory.ALL, cursor = null, limit = 20),
            )
        }

    @Test
    fun lastPageIsReturnedAsSuccess() =
        runTest {
            val repository =
                TeamNotificationRepositoryImpl(
                    FakeNotificationSource(
                        TeamNotificationPageDto(page = TeamNotificationCursorDto(nextCursor = null, hasMore = false)),
                    ),
                )

            val pageResult = repository.getNotifications(NotificationCategory.ALL, cursor = null, limit = 20)

            assertEquals(false, (pageResult as ApiResult.Success).value.hasMore)
        }

    private class FakeNotificationSource(
        private val notificationPage: TeamNotificationPageDto,
    ) : TeamNotificationRemoteDataSource {
        override suspend fun getNotifications(
            category: String,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPageDto> = ApiResult.Success(notificationPage)

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceiptDto> =
            ApiResult.Failure(ApiError.Unknown)

        override suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceiptDto> =
            ApiResult.Failure(ApiError.Unknown)
    }
}
