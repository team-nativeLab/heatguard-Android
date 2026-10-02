package com.nativelap.heartguard.viewmodel.notification

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationReadAllReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationReadReceipt
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.domain.notification.model.TeamNotificationPage
import com.nativelap.heartguard.domain.notification.repository.TeamNotificationRepository
import com.nativelap.heartguard.domain.notification.usecase.GetTeamNotificationsUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkAllTeamNotificationsReadUseCase
import com.nativelap.heartguard.domain.notification.usecase.MarkTeamNotificationReadUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.OffsetDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

    @Test
    fun `지연된 전체 읽음 중 재진입과 연타는 추가 요청을 만들지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        val pendingReadAll = CompletableDeferred<ApiResult<NotificationReadAllReceipt>>()
        repository.markAllHandler = { pendingReadAll.await() }
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markAllRead()
        runCurrent()

        viewModel.openNotifications()
        viewModel.markAllRead()
        viewModel.selectCategory(NotificationCategory.RECORD)
        runCurrent()
        assertEquals(1, repository.markAllRequests)
        assertEquals(1, repository.listRequests.size)
        assertTrue(viewModel.uiState.value.isMarkingAllRead)

        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00").copy(isRead = true))),
        )
        pendingReadAll.complete(ApiResult.Success(NotificationReadAllReceipt(1, 0)))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertFalse(viewModel.uiState.value.isMarkingAllRead)
    }

    @Test
    fun `전체 읽음 성공 후 조회 실패는 확정 결과를 보존하고 GET만 재시도한다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00")), cursor = "old"),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Failure(ApiError.Network)

        viewModel.markAllRead()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasRefreshError)
        assertFalse(viewModel.uiState.value.hasInitialError)
        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertEquals(null, viewModel.uiState.value.nextCursor)
        viewModel.loadNextPage()
        assertEquals(2, repository.listRequests.size)

        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(page())
        viewModel.retryRefresh()
        advanceUntilIdle()
        assertEquals(1, repository.markAllRequests)
        assertEquals(3, repository.listRequests.size)
        assertFalse(viewModel.uiState.value.hasRefreshError)
    }

    @Test
    fun `전체 읽음 실패는 목록과 미읽음 수를 보존하고 다시 요청할 수 있다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.markAllResult = ApiResult.Failure(ApiError.Network)
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markAllRead()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertFalse(viewModel.uiState.value.isMarkingAllRead)
        assertEquals(1, repository.listRequests.size)
        viewModel.markAllRead()
        advanceUntilIdle()
        assertEquals(2, repository.markAllRequests)
    }

    @Test
    fun `응답에 미읽음이 남으면 개별 읽음 상태를 추정하지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.markAllResult = ApiResult.Success(NotificationReadAllReceipt(0, 2))
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Failure(ApiError.Network)
        viewModel.markAllRead()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(2, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.hasRefreshError)
    }

    @Test
    fun `이전 세션의 늦은 PATCH가 새 세션 요청 추적을 지우지 않는다`() = runTest {
        val sessionManager = createSessionManager()
        val repository = FakeTeamNotificationRepository()
        val oldResponse = CompletableDeferred<ApiResult<NotificationReadAllReceipt>>()
        val newResponse = CompletableDeferred<ApiResult<NotificationReadAllReceipt>>()
        repository.markAllHandler = {
            if (repository.markAllRequests == 1) {
                withContext(NonCancellable) { oldResponse.await() }
            } else {
                newResponse.await()
            }
        }
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("old", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository, sessionManager)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markAllRead()
        runCurrent()
        sessionManager.expireSession()
        runCurrent()
        assertEquals(TeamNotificationUiState(), viewModel.uiState.value)
        sessionManager.onLoginSucceeded(SessionToken("new-token"))
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("new", "2026-10-01T12:00:00+09:00"))),
        )
        viewModel.openNotifications()
        runCurrent()
        viewModel.markAllRead()
        runCurrent()
        oldResponse.complete(ApiResult.Success(NotificationReadAllReceipt(1, 0)))
        runCurrent()
        viewModel.openNotifications()
        viewModel.markAllRead()
        runCurrent()
        assertEquals(2, repository.markAllRequests)
        assertEquals(2, repository.listRequests.size)
        assertEquals("new", viewModel.uiState.value.notifications.single().notificationId)
        assertTrue(viewModel.uiState.value.isMarkingAllRead)
        newResponse.complete(ApiResult.Failure(ApiError.Network))
        advanceUntilIdle()
    }

    @Test
    fun `전체 읽음 시작 전에 취소된 개별 응답은 카운트를 변경하지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        val pendingRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = { withContext(NonCancellable) { pendingRead.await() } }
        repository.markAllResult = ApiResult.Failure(ApiError.Network)
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markRead("n1")
        runCurrent()
        viewModel.markAllRead()
        runCurrent()
        pendingRead.complete(ApiResult.Success(NotificationReadReceipt("n1", true, null)))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(1, viewModel.uiState.value.unreadCount)
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    @Test
    fun `새로고침 중 재진입은 같은 조회를 유지하고 분류 변경은 오류를 지운다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-09-30T12:00:00+09:00"))),
        )
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        val pendingPage = CompletableDeferred<ApiResult<TeamNotificationPage>>()
        repository.pageHandler = { _, _ -> pendingPage.await() }
        viewModel.openNotifications()
        runCurrent()
        viewModel.openNotifications()
        viewModel.markAllRead()
        runCurrent()
        assertEquals(2, repository.listRequests.size)
        assertEquals(0, repository.markAllRequests)
        pendingPage.complete(ApiResult.Failure(ApiError.Network))
        advanceUntilIdle()
        repository.pageHandler = null
        viewModel.selectCategory(NotificationCategory.RECORD)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasRefreshError)
        assertEquals(NotificationCategory.RECORD, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `분류를 바꾼 뒤 늦게 끝난 이전 GET은 새 목록을 덮지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        val oldPage = CompletableDeferred<ApiResult<TeamNotificationPage>>()
        repository.pageHandler = { category, _ ->
            if (category == NotificationCategory.ALL) {
                withContext(NonCancellable) { oldPage.await() }
            } else {
                ApiResult.Success(page(items = listOf(notification("record", "2026-10-01T12:00:00+09:00"))))
            }
        }
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        runCurrent()
        viewModel.selectCategory(NotificationCategory.RECORD)
        runCurrent()
        oldPage.complete(
            ApiResult.Success(page(items = listOf(notification("old", "2026-09-30T12:00:00+09:00")))),
        )
        advanceUntilIdle()
        assertEquals(NotificationCategory.RECORD, viewModel.uiState.value.selectedCategory)
        assertEquals("record", viewModel.uiState.value.notifications.single().notificationId)
    }

    @Test
    fun `여러 읽음 처리 중 최신 분류 조회만 마지막 처리 뒤 실행한다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"),
                notification("n2", "2026-10-01T11:00:00+09:00")), cursor = "old"),
        )
        val firstRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        val secondRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = {
            if (repository.readRequests.size == 1) {
                firstRead.await()
            } else {
                secondRead.await()
            }
        }
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markRead("n1")
        runCurrent()
        viewModel.markRead("n2")
        runCurrent()
        viewModel.openNotifications()
        viewModel.selectCategory(NotificationCategory.RECORD)
        viewModel.selectCategory(NotificationCategory.NOTICE)
        viewModel.openNotifications()
        viewModel.loadNextPage()
        assertEquals(1, repository.listRequests.size)
        firstRead.complete(repository.markResult)
        runCurrent()
        assertEquals(1, repository.listRequests.size)
        assertTrue(viewModel.uiState.value.notifications.first().isRead)
        assertEquals(setOf("n2"), viewModel.uiState.value.markingReadIds)
        secondRead.complete(ApiResult.Failure(ApiError.Network))
        advanceUntilIdle()
        assertEquals(listOf(NotificationCategory.ALL, NotificationCategory.NOTICE),
            repository.listRequests.map { request -> request.category })
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    @Test
    fun `개별 읽음 성공 후 취소 무시한 pagination 응답은 확정 상태를 덮지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        val oldPage = CompletableDeferred<ApiResult<TeamNotificationPage>>()
        repository.pageHandler = { _, cursor ->
            if (cursor != null) {
                withContext(NonCancellable) { oldPage.await() }
            } else if (repository.listRequests.size == 1) {
                ApiResult.Success(page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00")), cursor = "old"))
            } else {
                ApiResult.Failure(ApiError.Network)
            }
        }
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        runCurrent()
        viewModel.loadNextPage()
        runCurrent()
        viewModel.markRead("n1")
        runCurrent()
        assertTrue(viewModel.uiState.value.hasRefreshError)
        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(0, viewModel.uiState.value.unreadCount)
        oldPage.complete(ApiResult.Success(page(unreadCount = 55, filteredUnreadCount = 55)))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notifications.single().isRead)
        assertEquals(0, viewModel.uiState.value.unreadCount)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(listOf(null, "old", null), repository.listRequests.map { request -> request.cursor })
    }

    @Test
    fun `일괄 처리로 취소된 개별 요청은 대기 분류 조회를 실행하지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"))),
        )
        val oldRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = { withContext(NonCancellable) { oldRead.await() } }
        val viewModel = createViewModel(repository)
        viewModel.openNotifications()
        runCurrent()
        viewModel.markRead("n1")
        runCurrent()
        viewModel.selectCategory(NotificationCategory.NOTICE)
        viewModel.markAllRead()
        runCurrent()
        oldRead.complete(repository.markResult)
        advanceUntilIdle()
        assertEquals(listOf(NotificationCategory.ALL, NotificationCategory.ALL),
            repository.listRequests.map { request -> request.category })
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    @Test
    fun `포그라운드 갱신은 5초 간격과 활성 요청을 지킨다`() = runTest {
        val clock = MutableClock()
        val repository = FakeTeamNotificationRepository()
        val viewModel = createViewModel(repository, clock = clock)
        viewModel.openNotifications()
        viewModel.refreshOnResume()
        advanceUntilIdle()
        assertEquals(1, repository.listRequests.size)
        clock.currentMillis = 4_999
        viewModel.refreshOnResume()
        advanceUntilIdle()
        assertEquals(1, repository.listRequests.size)
        val refreshPage = CompletableDeferred<ApiResult<TeamNotificationPage>>()
        repository.pageHandler = { _, _ -> refreshPage.await() }
        clock.currentMillis = 5_000
        viewModel.refreshOnResume()
        runCurrent()
        assertEquals(2, repository.listRequests.size)
        clock.currentMillis = 10_000
        viewModel.refreshOnResume()
        runCurrent()
        assertEquals(2, repository.listRequests.size)
        refreshPage.complete(ApiResult.Success(page()))
        advanceUntilIdle()
        repository.pageHandler = null
        viewModel.refreshOnResume()
        advanceUntilIdle()
        assertEquals(3, repository.listRequests.size)
    }

    @Test
    fun `개별 읽음 중 복귀한 갱신은 읽음 완료 후 자동으로 실행한다`() = runTest {
        val clock = MutableClock()
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"))),
        )
        val readResponse = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = { readResponse.await() }
        val viewModel = createViewModel(repository, clock = clock)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markRead("n1")
        runCurrent()
        clock.currentMillis = 5_000
        viewModel.refreshOnResume()
        assertEquals(1, repository.listRequests.size)
        readResponse.complete(repository.markResult)
        advanceUntilIdle()
        assertEquals(2, repository.listRequests.size)
        viewModel.refreshOnResume()
        advanceUntilIdle()
        assertEquals(2, repository.listRequests.size)
    }

    @Test
    fun `복귀 갱신은 대기 중 최신 분류를 덮어쓰지 않는다`() = runTest {
        val clock = MutableClock()
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"))),
        )
        val readResponse = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = { readResponse.await() }
        val viewModel = createViewModel(repository, clock = clock)
        viewModel.openNotifications()
        advanceUntilIdle()
        viewModel.markRead("n1")
        runCurrent()
        viewModel.selectCategory(NotificationCategory.NOTICE)
        clock.currentMillis = 5_000
        viewModel.refreshOnResume()
        readResponse.complete(repository.markResult)
        advanceUntilIdle()
        assertEquals(listOf(NotificationCategory.ALL, NotificationCategory.NOTICE),
            repository.listRequests.map { request -> request.category })
    }

    @Test
    fun `느린 오류 안내 collector는 여러 PATCH 실패 뒤 분류 조회를 막지 않는다`() = runTest {
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"),
                notification("n2", "2026-10-01T11:00:00+09:00"),
                notification("n3", "2026-10-01T10:00:00+09:00"))),
        )
        val firstFailedRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        val remainingFailedReads = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = {
            if (repository.readRequests.size == 1) {
                firstFailedRead.await()
            } else {
                remainingFailedReads.await()
            }
        }
        val viewModel = createViewModel(repository)
        val finishSnackbar = CompletableDeferred<Unit>()
        val receivedEffects = mutableListOf<TeamNotificationViewEffect>()
        backgroundScope.launch {
            viewModel.viewEffects.collect { effect ->
                receivedEffects += effect
                finishSnackbar.await()
            }
        }
        viewModel.openNotifications()
        runCurrent()
        viewModel.markRead("n1")
        viewModel.markRead("n2")
        viewModel.markRead("n3")
        viewModel.selectCategory(NotificationCategory.NOTICE)
        runCurrent()
        firstFailedRead.complete(ApiResult.Failure(ApiError.Network))
        runCurrent()
        remainingFailedReads.complete(ApiResult.Failure(ApiError.Network))
        runCurrent()
        assertEquals(listOf(NotificationCategory.ALL, NotificationCategory.NOTICE),
            repository.listRequests.map { request -> request.category })
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
        assertFalse(finishSnackbar.isCompleted)
        assertEquals(listOf(TeamNotificationViewEffect.ReadFailed), receivedEffects)
        finishSnackbar.complete(Unit)
        runCurrent()
        assertEquals(2, receivedEffects.size)
    }

    @Test
    fun `이전 세션 개별 요청 finally는 새 세션 대기 조회와 job을 건드리지 않는다`() = runTest {
        val sessionManager = createSessionManager()
        val repository = FakeTeamNotificationRepository()
        repository.pages[NotificationCategory.ALL to null] = ApiResult.Success(
            page(items = listOf(notification("n1", "2026-10-01T12:00:00+09:00"))),
        )
        val oldRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        val newRead = CompletableDeferred<ApiResult<NotificationReadReceipt>>()
        repository.markHandler = {
            if (repository.readRequests.size == 1) {
                withContext(NonCancellable) { oldRead.await() }
            } else {
                newRead.await()
            }
        }
        val viewModel = createViewModel(repository, sessionManager)
        viewModel.openNotifications()
        runCurrent()
        viewModel.markRead("n1")
        runCurrent()
        viewModel.selectCategory(NotificationCategory.NOTICE)
        sessionManager.expireSession()
        runCurrent()
        sessionManager.onLoginSucceeded(SessionToken("new-token"))
        runCurrent()
        viewModel.refreshOnResume()
        runCurrent()
        assertEquals(2, repository.listRequests.size)
        viewModel.markRead("n1")
        runCurrent()
        viewModel.selectCategory(NotificationCategory.RECORD)
        oldRead.complete(ApiResult.Failure(ApiError.Network))
        runCurrent()
        assertEquals(2, repository.listRequests.size)
        assertEquals(setOf("n1"), viewModel.uiState.value.markingReadIds)
        newRead.complete(repository.markResult)
        advanceUntilIdle()
        assertEquals(listOf(NotificationCategory.ALL, NotificationCategory.ALL, NotificationCategory.RECORD),
            repository.listRequests.map { request -> request.category })
        assertTrue(viewModel.uiState.value.markingReadIds.isEmpty())
    }

    private class MutableClock(var currentMillis: Long = 0) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis)
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
        clock: Clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneId.of("Asia/Seoul")),
    ): TeamNotificationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return TeamNotificationViewModel(
            getTeamNotificationsUseCase = GetTeamNotificationsUseCase(repository),
            markAllTeamNotificationsReadUseCase = MarkAllTeamNotificationsReadUseCase(repository),
            markTeamNotificationReadUseCase = MarkTeamNotificationReadUseCase(repository),
            sessionManager = sessionManager ?: createSessionManager(),
            clock = clock,
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
        var markAllRequests = 0
        var markAllResult: ApiResult<NotificationReadAllReceipt> =
            ApiResult.Success(NotificationReadAllReceipt(0, 0))
        var markAllHandler: (suspend () -> ApiResult<NotificationReadAllReceipt>)? = null
        var markHandler: (suspend () -> ApiResult<NotificationReadReceipt>)? = null
        var pageHandler: (suspend (NotificationCategory, String?) -> ApiResult<TeamNotificationPage>)? = null
        val pages = mutableMapOf<Pair<NotificationCategory, String?>, ApiResult<TeamNotificationPage>>()
        val listRequests = mutableListOf<ListRequest>()
        val readRequests = mutableListOf<String>()

        override suspend fun getNotifications(
            category: NotificationCategory,
            cursor: String?,
            limit: Int,
        ): ApiResult<TeamNotificationPage> {
            listRequests += ListRequest(category, cursor, limit)
            return pageHandler?.invoke(category, cursor) ?: pages[category to cursor] ?: ApiResult.Success(page())
        }

        override suspend fun markNotificationRead(notificationId: String): ApiResult<NotificationReadReceipt> {
            readRequests += notificationId
            return markHandler?.invoke() ?: markResult
        }

        override suspend fun markAllNotificationsRead(): ApiResult<NotificationReadAllReceipt> {
            markAllRequests += 1
            return markAllHandler?.invoke() ?: markAllResult
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
