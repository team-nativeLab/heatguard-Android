package com.nativelap.heartguard.view.screen.notification

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableStateOf
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationScreenEvent
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationUiState
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TeamNotificationScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reachingListEndDuringReadResumesAutomaticPaginationAfterReadCompletes() {
        val notification = TeamNotification(
            notificationId = "ntf_01",
            type = NotificationType.RECORD_CREATED,
            category = NotificationCategory.RECORD,
            title = "휴식 사진이 저장됐어요",
            resourceId = "rec_01",
            isRead = false,
            createdAt = OffsetDateTime.parse("2026-09-30T10:20:00+09:00"),
            updatedAt = null,
        )
        val screenState = mutableStateOf(TeamNotificationUiState(
            notifications = listOf(notification),
            hasLoaded = true,
            hasMore = true,
            nextCursor = "next",
            markingReadIds = setOf(notification.notificationId),
        ))
        var loadMoreRequests = 0
        composeTestRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(screenState.value, onEvent = { event ->
                    if (event == TeamNotificationScreenEvent.LoadMore) {
                        loadMoreRequests += 1
                    }
                })
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(0, loadMoreRequests)
        }
        val blockedStates = listOf(
            screenState.value.copy(markingReadIds = emptySet(), isMarkingAllRead = true),
            screenState.value.copy(markingReadIds = emptySet(), isRefreshing = true),
            screenState.value.copy(markingReadIds = emptySet(), hasRefreshError = true),
        )
        blockedStates.forEach { blockedState ->
            composeTestRule.runOnIdle { screenState.value = blockedState }
            composeTestRule.waitForIdle()
            composeTestRule.runOnIdle { assertEquals(0, loadMoreRequests) }
        }
        composeTestRule.runOnIdle {
            screenState.value = screenState.value.copy(hasRefreshError = false)
        }
        composeTestRule.waitUntil(5_000) { loadMoreRequests == 1 }
        composeTestRule.runOnIdle { assertEquals(1, loadMoreRequests) }
    }

    @Test
    fun emptyListShowsFiltersAndLetsWorkerChooseCategory() {
        var receivedEvent: TeamNotificationScreenEvent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(
                    uiState = TeamNotificationUiState(hasLoaded = true),
                    onEvent = { event -> receivedEvent = event },
                )
            }
        }

        composeTestRule.onNodeWithText("알림").assertExists()
        composeTestRule.onNodeWithText("아직 받은 알림이 없어요").assertExists()
        composeTestRule.onNodeWithText("폭염 경보나 기록 알림이 오면 여기에 모아서 보여드려요").assertExists()
        composeTestRule.onNodeWithText("기록").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                TeamNotificationScreenEvent.CategorySelected(NotificationCategory.RECORD),
                receivedEvent,
            )
        }
    }

    @Test
    fun refreshFailureRemainsVisibleOnEmptyListAndRetryOnlyRequestsRefresh() {
        var receivedEvent: TeamNotificationScreenEvent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(
                    uiState = TeamNotificationUiState(hasLoaded = true, hasRefreshError = true),
                    onEvent = { event -> receivedEvent = event },
                )
            }
        }
        composeTestRule.onNodeWithText("아직 받은 알림이 없어요").assertExists()
        composeTestRule.onNodeWithText("목록을 새로 불러오지 못했어요. 다시 시도해 주세요.").assertExists()
        composeTestRule.onNodeWithText("재시도").performClick()
        composeTestRule.runOnIdle {
            assertEquals(TeamNotificationScreenEvent.RetryRefresh, receivedEvent)
        }
    }

    @Test
    fun unreadNotificationShowsTitleAndClickRequestsRead() {
        var receivedEvent: TeamNotificationScreenEvent? = null
        val notification = TeamNotification(
            notificationId = "ntf_01",
            type = NotificationType.RECORD_CREATED,
            category = NotificationCategory.RECORD,
            title = "휴식 사진이 저장됐어요",
            resourceId = "rec_01",
            isRead = false,
            createdAt = OffsetDateTime.parse("2026-09-30T10:20:00+09:00"),
            updatedAt = null,
        )
        composeTestRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(
                    uiState = TeamNotificationUiState(
                        notifications = listOf(notification),
                        hasLoaded = true,
                    ),
                    onEvent = { event -> receivedEvent = event },
                )
            }
        }

        composeTestRule.onNodeWithText("휴식 사진이 저장됐어요").performClick()

        composeTestRule.runOnIdle {
            assertEquals(TeamNotificationScreenEvent.NotificationClicked("ntf_01"), receivedEvent)
        }
    }

    @Test
    fun readNotificationCanStillBeClickedToOpenRelatedScreen() {
        var receivedEvent: TeamNotificationScreenEvent? = null
        val notification = TeamNotification(
            notificationId = "ntf_02",
            type = NotificationType.INQUIRY_ANSWERED,
            category = NotificationCategory.NOTICE,
            title = "문의에 답변이 등록됐어요",
            resourceId = "inq_01",
            isRead = true,
            createdAt = OffsetDateTime.parse("2026-09-30T10:20:00+09:00"),
            updatedAt = null,
        )
        composeTestRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(
                    uiState = TeamNotificationUiState(
                        notifications = listOf(notification),
                        hasLoaded = true,
                        isRefreshing = true,
                    ),
                    onEvent = { event -> receivedEvent = event },
                )
            }
        }

        composeTestRule.onNodeWithText("문의에 답변이 등록됐어요").performClick()

        composeTestRule.runOnIdle {
            assertEquals(TeamNotificationScreenEvent.NotificationClicked("ntf_02"), receivedEvent)
        }
    }
}
