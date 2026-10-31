package com.nativelap.heartguard.view.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class HomeFixedRecordActionUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recordActionMatchesFigmaWidthAndRespectsNavigationInset() {
        val navigationInsetPx = setHomeScreen(width = 402.dp, height = 978.dp)
        val screenBounds = composeRule.onNodeWithTag(HOME_SCREEN_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val actionBounds = composeRule.onNodeWithTag(HOME_RECORD_ACTION_TEST_TAG).fetchSemanticsNode().boundsInRoot

        assertNear(actionBounds.left - screenBounds.left, with(composeRule.density) { 28.dp.toPx() })
        assertNear(actionBounds.width, with(composeRule.density) { 346.dp.toPx() })
        assertNear(actionBounds.height, with(composeRule.density) { 48.dp.toPx() })
        assertTrue(actionBounds.bottom <= screenBounds.bottom)
        val expectedBottomInset = navigationInsetPx + with(composeRule.density) { 12.dp.toPx() }
        assertNear(
            actionBounds.top - screenBounds.top,
            screenBounds.height - expectedBottomInset - actionBounds.height,
        )
        assertNear(
            screenBounds.bottom - actionBounds.bottom,
            expectedBottomInset,
        )
    }

    @Test
    fun recordActionStaysFixedWhileShortLargeTextHomeListScrollsAndCallsBackOnce() {
        val recordClickCount = mutableIntStateOf(0)
        setHomeScreen(
            width = 320.dp,
            height = 640.dp,
            fontScale = 1.5f,
            onRecordClick = { recordClickCount.intValue += 1 },
        )

        val actionNode = composeRule.onNodeWithTag(HOME_RECORD_ACTION_TEST_TAG)
        actionNode.assertHasClickAction()
        val initialActionBounds = actionNode.fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithTag(HOME_SCROLL_CONTENT_TEST_TAG)
            .performScrollToIndex(HOME_RECORD_HISTORY_ITEM_INDEX)
        composeRule.waitForIdle()

        val scrolledActionBounds = actionNode.fetchSemanticsNode().boundsInRoot
        assertNear(scrolledActionBounds.left, initialActionBounds.left)
        assertNear(scrolledActionBounds.top, initialActionBounds.top)
        val lastRecordCardBounds =
            composeRule.onNodeWithTag(HOME_RECORD_HISTORY_CARD_TEST_TAG)
                .fetchSemanticsNode().boundsInRoot
        assertTrue(
            lastRecordCardBounds.bottom + with(composeRule.density) { 10.dp.toPx() } <=
                scrolledActionBounds.top,
        )

        actionNode.performClick()

        assertEquals(1, recordClickCount.intValue)
    }

    private fun setHomeScreen(
        width: Dp,
        height: Dp,
        fontScale: Float = 1f,
        onRecordClick: () -> Unit = {},
    ): Float {
        var navigationInsetPx = 0f
        composeRule.setContent {
            val systemDensity = LocalDensity.current
            navigationInsetPx = WindowInsets.navigationBars.getBottom(systemDensity).toFloat()
            val densityWithRequestedFontScale =
                remember(systemDensity, fontScale) {
                    Density(systemDensity.density, fontScale)
                }

            CompositionLocalProvider(LocalDensity provides densityWithRequestedFontScale) {
                HeartGuardTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        HomeScreen(
                            currentTemperature = "33.2°C",
                            feelsLikeTemperature = "36.1°C",
                            humidity = "61%",
                            temperatureDelta = "+1.2°C",
                            isTemperatureIncreasing = true,
                            isTemperatureDeltaZero = false,
                            weatherValue = "맑음",
                            weatherImageRes = com.nativelap.heartguard.R.drawable.weather_sunny,
                            riskLabel = "폭염 주의 단계",
                            nextCheckDescription = "다음 체크까지 57분 · 22:00 예정",
                            checkTimelineItems =
                                listOf(
                                    CheckTimelineItem("08시", isCompleted = true),
                                    CheckTimelineItem("10시", isCompleted = false),
                                    CheckTimelineItem("12시", isCompleted = false, isCurrent = true),
                                    CheckTimelineItem("14시", isCompleted = false),
                                ),
                            onMenuClick = {},
                            onNotificationClick = {},
                            onManagerCallClick = {},
                            onEmergencyClick = {},
                            onFieldPhotoClick = {},
                            onRecordHistoryClick = {},
                            onChecklistClick = {},
                            onRecordClick = onRecordClick,
                            modifier =
                                Modifier
                                    .requiredSize(width, height)
                                    .testTag(HOME_SCREEN_TEST_TAG),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        return navigationInsetPx
    }

    private fun assertNear(
        actual: Float,
        expected: Float,
    ) {
        val tolerance = with(composeRule.density) { 1.dp.toPx() }
        assertTrue("actual=$actual expected=$expected", abs(actual - expected) <= tolerance)
    }

    private companion object {
        const val HOME_SCREEN_TEST_TAG = "home-screen"
        const val HOME_SCROLL_CONTENT_TEST_TAG = "home-scroll-content"
        const val HOME_RECORD_ACTION_TEST_TAG = "home-record-action"
        const val HOME_RECORD_HISTORY_CARD_TEST_TAG = "home-record-history-card"
        const val HOME_RECORD_HISTORY_ITEM_INDEX = 7
    }
}
