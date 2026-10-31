package com.nativelap.heartguard.viewmodel.notification

import androidx.lifecycle.ViewModelStore
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamNotificationUnreadViewModelTest {
    @Test
    fun `refresh uses server unread count and existing first page contract`() = runTest {
        val fixture = createFixture()
        try {
            fixture.repository.response = {
                ApiResult.Success(notificationPage(unreadCount = 6))
            }

            fixture.viewModel.refresh()
            runCurrent()

            assertEquals(6, fixture.viewModel.uiState.value.unreadCount)
            assertTrue(fixture.viewModel.uiState.value.hasUnread)
            assertEquals(
                listOf(NotificationRequest(NotificationCategory.ALL, cursor = null, limit = 20)),
                fixture.repository.requests,
            )
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `failed refresh preserves the last server confirmed count`() = runTest {
        val fixture = createFixture()
        try {
            fixture.repository.response = {
                ApiResult.Success(notificationPage(unreadCount = 3))
            }
            fixture.viewModel.refresh()
            runCurrent()
            assertEquals(3, fixture.viewModel.uiState.value.unreadCount)

            fixture.repository.response = {
                ApiResult.Failure(ApiError.Network)
            }
            fixture.viewModel.refresh()
            runCurrent()

            assertEquals(3, fixture.viewModel.uiState.value.unreadCount)
            assertTrue(fixture.viewModel.uiState.value.hasUnread)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `session end clears unread state and cancels the active refresh`() = runTest {
        val fixture = createFixture()
        try {
            fixture.repository.response = {
                ApiResult.Success(notificationPage(unreadCount = 5))
            }
            fixture.viewModel.refresh()
            runCurrent()
            assertEquals(5, fixture.viewModel.uiState.value.unreadCount)

            val pendingResult = CompletableDeferred<ApiResult<TeamNotificationPage>>()
            var cancellationObserved = false
            fixture.repository.response = {
                try {
                    pendingResult.await()
                } catch (exception: CancellationException) {
                    cancellationObserved = true
                    throw exception
                }
            }
            fixture.viewModel.refresh()
            runCurrent()

            fixture.sessionManager.expireSession()
            runCurrent()

            assertNull(fixture.viewModel.uiState.value.unreadCount)
            assertFalse(fixture.viewModel.uiState.value.hasUnread)
            assertTrue(cancellationObserved)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `success from a previous session is not applied after session generation changes`() = runTest {
        val fixture = createFixture()
        try {
            fixture.repository.response = {
                fixture.sessionManager.expireSession()
                ApiResult.Success(notificationPage(unreadCount = 99))
            }

            fixture.viewModel.refresh()
            runCurrent()

            assertNull(fixture.viewModel.uiState.value.unreadCount)
            assertFalse(fixture.viewModel.uiState.value.hasUnread)
        } finally {
            fixture.close()
        }
    }

    private suspend fun TestScope.createFixture(): Fixture {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sessionManager =
            SessionManager(
                tokenStorage = InMemoryTokenStorage(),
                ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        val repository = FakeTeamNotificationRepository()
        val viewModel =
            TeamNotificationUnreadViewModel(
                getTeamNotificationsUseCase = GetTeamNotificationsUseCase(repository),
                sessionManager = sessionManager,
            )
        val viewModelStore = ViewModelStore().apply {
            put("notification-unread-test", viewModel)
        }
        sessionManager.onLoginSucceeded(SessionToken("test-session-token"))
        runCurrent()

        return Fixture(sessionManager, repository, viewModel, viewModelStore)
    }

    private fun notificationPage(unreadCount: Int): TeamNotificationPage =
        TeamNotificationPage(
            items = emptyList(),
            nextCursor = null,
            hasMore = false,
            unreadCount = unreadCount,
            filteredUnreadCount = unreadCount,
        )

    private data class NotificationRequest(
        val category: NotificationCategory,
        val cursor: String?,
        val limit: Int,
    )

    private class FakeTeamNotificationRepository : TeamNotificationRepository {
        val requests = mutableListOf<NotificationRequest>()
        var response: suspend () -> ApiResult<TeamNotificationPage> = {
            ApiResult.Success(
                TeamNotificationPage(
                    items = emptyList(),
                    nextCursor = null,
                    hasMore = false,
                    unreadCount = 0,
                    filteredUnreadCount = 0,
                ),
            )
        }

        override suspend fun getNotifications(
            category: NotificationCategory,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPage> {
            requests += NotificationRequest(category, cursor, limit)
            return response()
        }

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceipt> =
            error("Single notification read is not used in these tests")

        override suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceipt> =
            error("Mark all read is not used in these tests")
    }

    private class InMemoryTokenStorage : TokenStorage {
        private var accessToken: String? = null

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }

    private data class Fixture(
        val sessionManager: SessionManager,
        val repository: FakeTeamNotificationRepository,
        val viewModel: TeamNotificationUnreadViewModel,
        val viewModelStore: ViewModelStore,
    ) {
        fun close() {
            viewModelStore.clear()
            Dispatchers.resetMain()
        }
    }
}
