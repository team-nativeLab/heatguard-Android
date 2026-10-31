package com.nativelap.heartguard.view.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.screen.home.HomeScreen
import com.nativelap.heartguard.view.screen.notification.TeamNotificationScreen
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class NotificationUnreadIndicatorUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeBellShowsRedDotAndUnreadAccessibilityDescriptionWhenUnreadExists() {
        composeRule.setContent {
            HeartGuardTheme {
                HomeScreenUnderTest(hasUnreadNotifications = true)
            }
        }
        composeRule.waitForIdle()

        val bellNode = composeRule.onNodeWithContentDescription("알림, 읽지 않은 알림 있음")
        bellNode.assertExists()

        val bellBounds = bellNode.fetchSemanticsNode().boundsInRoot
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val dotX = bellBounds.center.x.roundToInt()
        val dotY = (bellBounds.bottom - with(composeRule.density) { 6.dp.toPx() }).roundToInt()

        assertEquals(Color(0xFFE53935), image[dotX, dotY])
    }

    @Test
    fun homeBellHidesRedDotAndUsesDefaultAccessibilityDescriptionWhenNoUnreadExists() {
        composeRule.setContent {
            HeartGuardTheme {
                HomeScreenUnderTest(hasUnreadNotifications = false)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("알림, 읽지 않은 알림 있음").assertDoesNotExist()
        val bellNode = composeRule.onNodeWithContentDescription("알림")
        bellNode.assertExists()

        val bellBounds = bellNode.fetchSemanticsNode().boundsInRoot
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val dotX = bellBounds.center.x.roundToInt()
        val dotY = (bellBounds.bottom - with(composeRule.density) { 6.dp.toPx() }).roundToInt()

        assertTrue(image[dotX, dotY] != Color(0xFFE53935))
    }

    @Test
    fun markAllReadButtonSharesTitleRowAndIsAlignedOnTheRight() {
        composeRule.setContent {
            HeartGuardTheme {
                TeamNotificationScreen(
                    uiState =
                        TeamNotificationUiState(
                            selectedCategory = NotificationCategory.ALL,
                            hasLoaded = true,
                            unreadCount = 4,
                        ),
                    onEvent = {},
                )
            }
        }
        composeRule.waitForIdle()

        val titleBounds = composeRule.onNodeWithText("알림").fetchSemanticsNode().boundsInRoot
        val markAllReadBounds =
            composeRule.onNodeWithText("모두 읽음").fetchSemanticsNode().boundsInRoot

        assertTrue(markAllReadBounds.center.x > titleBounds.center.x)
        assertTrue(
            abs(markAllReadBounds.center.y - titleBounds.center.y) <=
                with(composeRule.density) { 4.dp.toPx() },
        )
    }

    @Composable
    private fun HomeScreenUnderTest(hasUnreadNotifications: Boolean) {
        HomeScreen(
            currentTemperature = "--.- °C",
            feelsLikeTemperature = "--.- °C",
            humidity = "--%",
            temperatureDelta = "--.- °C",
            isTemperatureIncreasing = null,
            isTemperatureDeltaZero = false,
            weatherValue = "--",
            weatherImageRes = R.drawable.heartguard_home_weather,
            riskLabel = "--",
            nextCheckDescription = "--",
            checkTimelineItems = emptyList<CheckTimelineItem>(),
            onMenuClick = {},
            onNotificationClick = {},
            onManagerCallClick = {},
            onEmergencyClick = {},
            onFieldPhotoClick = {},
            onRecordHistoryClick = {},
            onChecklistClick = {},
            onRecordClick = {},
            hasUnreadNotifications = hasUnreadNotifications,
        )
    }
}
