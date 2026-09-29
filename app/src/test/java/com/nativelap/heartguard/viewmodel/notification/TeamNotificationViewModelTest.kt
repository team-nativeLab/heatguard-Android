package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkTeamNotificationReadUseCase
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamNotificationViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `분류를 바꾸면 새 분류의 첫 페이지부터 요청한다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("all-1", "2026-09-30T10:00:00+09:00")), cursor = "old-cursor"),
        )
        repository.pages[NotificationCategory.RECORD to null] = ApiResult.Success(
            page(items = listOf(notification("record-1", "2026-09-30T11:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)

        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.selectCategory(NotificationCategory.RECORD)
        advanceUntilIdle()

        assertEquals(
            listOf(NotificationCategory.ALL to null, NotificationCategory.RECORD to null),
            repository.listRequests.map { request -> request.category to request.cursor },
        )
        assertEquals(NotificationCategory.RECORD, viewModel.uiState.value.selectedCategory)
        assertEquals(listOf("record-1"), viewModel.uiState.value.notifications.map { item -> item.notificationId })
        assertEquals(true, viewModel.uiState.value.hasLoaded)
    }

    @Test
    fun `다음 cursor 페이지를 합치고 중복 알림을 제거한다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(
                items = listOf(
                    notification("newest", "2026-09-30T12:00:00+09:00"),
                    notification("duplicate", "2026-09-30T11:00:00+09:00"),
                ),
                cursor = "cursor-1",
                unreadCount = 3,
            ),
        )
        repository.pages[NotificationCategory.ALL to "cursor-1"] = ApiResult.Success(
            page(
                items = listOf(
                    notification("duplicate", "2026-09-30T11:00:00+09:00"),
                    notification("oldest", "2026-09-30T10:00:00+09:00"),
                ),
                unreadCount = 3,
            ),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(listOf("newest", "duplicate", "oldest"), viewModel.uiState.value.notifications.map { item -> item.notificationId })
        assertEquals(listOf(null, "cursor-1"), repository.listRequests.map { request -> request.cursor })
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(3, viewModel.uiState.value.unreadCount)
    }

    @Test
    fun `읽음 처리 성공 시 한 번만 PATCH하고 전체 및 분류 미읽음 수를 줄인다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00")), unreadCount = 2, filteredUnreadCount = 2),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()

        viewModel.markRead("n1")
        viewModel.markRead("n1")
        advanceUntilIdle()
        viewModel.markRead("n1")
        advanceUntilIdle()

        assertEquals(listOf("n1"), repository.readRequests)
        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertEquals(1, viewModel.uiState.value.filteredUnreadCount)
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    @Test
    fun `읽음 처리 실패 시 행과 미읽음 수를 그대로 둔다`() = runTest {
        val repository = FakeTeamNotificationRepository(
            markResult = ApiResult.Failure(ApiError.Network),
        )
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00")), unreadCount = 1, filteredUnreadCount = 1),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()

        viewModel.markRead("n1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    @Test
    fun `세션이 종료되면 알림 목록과 읽지 않은 수를 비운다`() = runTest {
        val sessionManager = createSessionManager()
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00")), unreadCount = 1),
        )
        val viewModel = createViewModel(repository, sessionManager)
        viewModel.openNotifications()
        advanceUntilIdle()

        sessionManager.expireSession()
        advanceUntilIdle()

        assertEquals(TeamNotificationUiState(), viewModel.uiState.value)
    }

    private suspend fun TestScope.createSessionManager(): SessionManager {
        val sessionManager = SessionManager(
            tokenStorage = FakeTokenStorage(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        sessionManager.initialize()
        return sessionManager
    }

    private suspend fun TestScope.createViewModel(
        repository: FakeTeamNotificationRepository,
        sessionManager: SessionManager? = null,
    ): TeamNotificationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return TeamNotificationViewModel(
            getTeamNotificationsUseCase = GetTeamNotificationsUseCase(repository),
            markTeamNotificationReadUseCase = MarkTeamNotificationReadUseCase(repository),
            sessionManager = sessionManager ?: createSessionManager(),
        )
    }

    private fun page(
        items: List<TeamNotification> = emptyList(),
        cursor: String? = null,
        unreadCount: Int = items.count { item -> !item.isRead },
        filteredUnreadCount: Int = unreadCount,
    ) = TeamNotificationPage(
        items = items,
        nextCursor = cursor,
        hasMore = cursor != null,
        unreadCount = unreadCount,
        filteredUnreadCount = filteredUnreadCount,
    )

    private fun notification(
        id: String,
        createdAt: String,
    ) = TeamNotification(
        notificationId = id,
        type = NotificationType.RECORD_CREATED,
        category = NotificationCategory.RECORD,
        title = id,
        resourceId = "record-$id",
        isRead = false,
        createdAt = OffsetDateTime.parse(createdAt),
        updatedAt = null,
    )

    private inner class FakeTeamNotificationRepository(
        var markResult: ApiResult<NotificationReadReceipt> = ApiResult.Success(
            NotificationReadReceipt(
                notificationId = "n1",
                isRead = true,
                readAt = OffsetDateTime.parse("2026-09-30T12:01:00+09:00"),
            ),
        ),
    ) : TeamNotificationRepository {
        val pages = mutableMapOf<Pair<NotificationCategory, String?>, ApiResult<TeamNotificationPage>>()
        val listRequests = mutableListOf<ListRequest>()
        val readRequests = mutableListOf<String>()

        override suspend fun getNotifications(
            category: NotificationCategory,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPage> {
            listRequests += ListRequest(category, cursor, limit)
            return pages[category to cursor] ?: ApiResult.Success(page())
        }

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceipt> {
            readRequests += notificationId
            return markResult
        }
    }

    private data class ListRequest(
        val category: NotificationCategory,
        val cursor: String?,
        val limit: Int,
    )

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = "access-token"

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
