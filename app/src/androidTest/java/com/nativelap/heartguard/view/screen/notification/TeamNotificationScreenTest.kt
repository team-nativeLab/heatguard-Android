package com.nativelap.heartguard.view.screen.notification

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
