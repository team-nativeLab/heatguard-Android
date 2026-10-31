package com.nativelap.heartguard.view.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.home.CheckTimelineItem
import com.nativelap.heartguard.view.component.home.HomeWeatherMetricsCard
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
    fun homeWeatherUnitsStayBesideTheirValuesAtFigmaCardWidth() {
        setWeatherMetricsCard(cardWidth = 320.dp)

        assertSameLineAndUnitFollows(
            value = "41",
            unit = "%",
            cardWidth = 320.dp,
            columnStart = 62.dp,
            columnEnd = 98.dp,
            checkLineHeights = true,
        )
        assertSameLineAndUnitFollows(
            value = "24.8",
            unit = "°C",
            cardWidth = 320.dp,
            columnStart = 159.dp,
            columnEnd = 205.dp,
            checkLineHeights = true,
        )
        assertFigmaCardBounds()
        assertFigmaMetricColumnPositions()
    }

    @Test
    fun homeWeatherUnitsStayBesideTheirValuesWithinNarrowCardWidth() {
        setWeatherMetricsCard(cardWidth = 278.dp)

        assertSameLineAndUnitFollows("41", "%", cardWidth = 278.dp)
        assertSameLineAndUnitFollows("24.8", "°C", cardWidth = 278.dp)
        assertMetricLabelsAreStacked()
    }

    @Test
    fun homeWeatherMetricsStayHorizontalAtTypicalHomeCardWidth() {
        setWeatherMetricsCard(
            cardWidth = 311.dp,
            humidityValue = "100%",
        )

        assertSameLineAndUnitFollows(
            value = "100",
            unit = "%",
            cardWidth = 311.dp,
            columnStart = 62.dp,
            columnEnd = 98.dp,
        )
        assertSameLineAndUnitFollows(
            value = "24.8",
            unit = "°C",
            cardWidth = 311.dp,
            columnStart = 159.dp,
            columnEnd = 205.dp,
        )
        assertMetricGroupsShareRow()
    }

    @Test
    fun homeWeatherUnitsStayBesideTheirValuesWithLargeFontScale() {
        setWeatherMetricsCard(cardWidth = 320.dp, fontScale = 1.25f)

        assertSameLineAndUnitFollows("41", "%", cardWidth = 320.dp)
        assertSameLineAndUnitFollows("24.8", "°C", cardWidth = 320.dp)
        assertMetricLabelsAreStacked()
    }

    @Test
    fun homeWeatherUnitsStayBesideTheirValuesInWideCardWithLargeFontScale() {
        setWeatherMetricsCard(cardWidth = 402.dp, fontScale = 1.25f)

        assertSameLineAndUnitFollows("41", "%", cardWidth = 402.dp)
        assertSameLineAndUnitFollows("24.8", "°C", cardWidth = 402.dp)
        assertMetricWrappersShareRow()
    }

    @Test
    fun homeWeatherCardUsesFigmaMetricColors() {
        setWeatherMetricsCard(cardWidth = 320.dp)

        val image = composeRule.onRoot().captureToImage().toPixelMap()
        assertTrue(containsColor(image, Color(0xFF6B7280)))
        assertTrue(containsColor(image, Color(0xFF30333D)))
        assertTrue(containsColor(image, Color(0xFFE8EBF5)))
        assertTrue(containsIconColor(image, "home-weather-icon-humidity", Color(0xFF4470ED)))
        assertTrue(containsIconColor(image, "home-weather-icon-humidity", Color(0xFFA0C4F4)))
        assertTrue(containsIconColor(image, "home-weather-icon-feels-like", Color(0xFF4470ED)))
        assertTrue(containsIconColor(image, "home-weather-icon-weather", Color(0xFF4470ED)))
    }

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
        bellNode.assertHasClickAction()

        val bellBounds = bellNode.fetchSemanticsNode().boundsInRoot
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val dotX = (bellBounds.right - with(composeRule.density) { 10.dp.toPx() }).roundToInt()
        val dotY = (bellBounds.top + with(composeRule.density) { 10.dp.toPx() }).roundToInt()

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
        bellNode.assertHasClickAction()

        val bellBounds = bellNode.fetchSemanticsNode().boundsInRoot
        val image = composeRule.onRoot().captureToImage().toPixelMap()
        val dotX = (bellBounds.right - with(composeRule.density) { 10.dp.toPx() }).roundToInt()
        val dotY = (bellBounds.top + with(composeRule.density) { 10.dp.toPx() }).roundToInt()

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

    private fun setWeatherMetricsCard(
        cardWidth: Dp,
        fontScale: Float = 1f,
        humidityValue: String = "41%",
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                HeartGuardTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(cardWidth).testTag("home-weather-metrics-card")) {
                            HomeWeatherMetricsCard(
                                humidityLabel = "습도",
                                humidity = humidityValue,
                                feelsLikeTemperatureLabel = "체감온도",
                                feelsLikeTemperature = "24.8°C",
                                weatherLabel = "날씨",
                                weatherValue = "맑음",
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertSameLineAndUnitFollows(
        value: String,
        unit: String,
        cardWidth: Dp,
        columnStart: Dp? = null,
        columnEnd: Dp? = null,
        checkLineHeights: Boolean = false,
    ) {
        val valueBounds = composeRule.onNodeWithText(value).fetchSemanticsNode().boundsInRoot
        val unitBounds = composeRule.onNodeWithText(unit).fetchSemanticsNode().boundsInRoot
        val maxCenterDelta = with(composeRule.density) { 3.dp.toPx() }
        val cardWidthPx = with(composeRule.density) { cardWidth.toPx() }

        assertTrue(valueBounds.width > 0f)
        assertTrue(unitBounds.width > 0f)
        assertTrue(abs(valueBounds.center.y - unitBounds.center.y) <= maxCenterDelta)
        assertTrue(unitBounds.left >= valueBounds.right)
        assertTrue(valueBounds.left >= 0f)
        assertTrue(unitBounds.right <= cardWidthPx)
        columnStart?.let { expectedStart ->
            assertNear(valueBounds.left, with(composeRule.density) { expectedStart.toPx() })
        }
        columnEnd?.let { expectedEnd ->
            assertTrue(unitBounds.right <= with(composeRule.density) { expectedEnd.toPx() })
        }
        if (checkLineHeights) {
            assertTextHeight(value, expectedHeight = 19.sp)
            assertTextHeight(unit, expectedHeight = 19.sp)
        }
    }

    private fun assertFigmaCardBounds() {
        val cardBounds = composeRule.onNodeWithTag("home-weather-metrics-card").fetchSemanticsNode().boundsInRoot
        assertNear(cardBounds.width, with(composeRule.density) { 320.dp.toPx() })
        assertNear(cardBounds.height, with(composeRule.density) { 78.dp.toPx() })
    }

    private fun assertMetricWrappersShareRow() {
        val metricBounds =
            listOf(
                "home-weather-metric-humidity",
                "home-weather-metric-feels-like",
                "home-weather-metric-weather",
            ).map { tag ->
                composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            }
        val centerTolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(abs(metricBounds[0].center.y - metricBounds[1].center.y) <= centerTolerance)
        assertTrue(abs(metricBounds[1].center.y - metricBounds[2].center.y) <= centerTolerance)
        assertNear(metricBounds[0].width, with(composeRule.density) { 89.dp.toPx() })
        assertNear(metricBounds[1].width, with(composeRule.density) { 101.5.dp.toPx() })
        assertNear(metricBounds[2].width, with(composeRule.density) { 76.5.dp.toPx() })
    }

    private fun assertMetricGroupsShareRow() {
        val metricBounds =
            listOf(
                "home-weather-metric-humidity",
                "home-weather-metric-feels-like",
                "home-weather-metric-weather",
            ).map { tag ->
                composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            }
        val centerTolerance = with(composeRule.density) { 1.dp.toPx() }

        assertTrue(abs(metricBounds[0].center.y - metricBounds[1].center.y) <= centerTolerance)
        assertTrue(abs(metricBounds[1].center.y - metricBounds[2].center.y) <= centerTolerance)
        assertTrue(metricBounds[0].right < metricBounds[1].left)
        assertTrue(metricBounds[1].right < metricBounds[2].left)
    }

    private fun containsColor(
        pixels: PixelMap,
        expectedColor: Color,
    ): Boolean =
        (0 until pixels.height).any { y ->
            (0 until pixels.width).any { x -> pixels[x, y] == expectedColor }
        }

    private fun containsIconColor(
        pixels: PixelMap,
        iconTestTag: String,
        expectedColor: Color,
    ): Boolean {
        val bounds =
            composeRule
                .onNodeWithTag(iconTestTag)
                .fetchSemanticsNode()
                .boundsInRoot
        val left = bounds.left.roundToInt().coerceAtLeast(0)
        val top = bounds.top.roundToInt().coerceAtLeast(0)
        val right = bounds.right.roundToInt().coerceAtMost(pixels.width)
        val bottom = bounds.bottom.roundToInt().coerceAtMost(pixels.height)

        return (top until bottom).any { y ->
            (left until right).any { x -> pixels[x, y] == expectedColor }
        }
    }

    private fun assertFigmaMetricColumnPositions() {
        val humidityMetric =
            composeRule
                .onNodeWithTag("home-weather-metric-humidity")
                .fetchSemanticsNode()
                .boundsInRoot
        val firstDivider =
            composeRule
                .onNodeWithTag("home-weather-divider-first")
                .fetchSemanticsNode()
                .boundsInRoot
        val feelsLikeMetric =
            composeRule
                .onNodeWithTag("home-weather-metric-feels-like")
                .fetchSemanticsNode()
                .boundsInRoot
        val secondDivider =
            composeRule
                .onNodeWithTag("home-weather-divider-second")
                .fetchSemanticsNode()
                .boundsInRoot
        val weatherMetric =
            composeRule
                .onNodeWithTag("home-weather-metric-weather")
                .fetchSemanticsNode()
                .boundsInRoot
        val humidityIcon =
            composeRule
                .onNodeWithTag("home-weather-icon-humidity")
                .fetchSemanticsNode()
                .boundsInRoot
        val feelsLikeIcon =
            composeRule
                .onNodeWithTag("home-weather-icon-feels-like")
                .fetchSemanticsNode()
                .boundsInRoot
        val weatherIcon =
            composeRule
                .onNodeWithTag("home-weather-icon-weather")
                .fetchSemanticsNode()
                .boundsInRoot
        val humidityLabel = composeRule.onNodeWithText("습도").fetchSemanticsNode().boundsInRoot
        val feelsLikeLabel = composeRule.onNodeWithText("체감온도").fetchSemanticsNode().boundsInRoot
        val weatherLabel = composeRule.onNodeWithText("날씨").fetchSemanticsNode().boundsInRoot
        val density = composeRule.density

        assertBounds(humidityMetric, left = 18.dp, width = 80.dp)
        assertBounds(firstDivider, left = 106.dp, width = 1.dp)
        assertBounds(feelsLikeMetric, left = 115.dp, width = 90.dp)
        assertBounds(secondDivider, left = 213.dp, width = 1.dp)
        assertBounds(weatherMetric, left = 222.dp, width = 70.dp)
        assertBounds(humidityIcon, left = 18.dp, width = 36.dp)
        assertBounds(feelsLikeIcon, left = 115.dp, width = 36.dp)
        assertBounds(weatherIcon, left = 222.dp, width = 36.dp)
        assertNear(firstDivider.left - humidityMetric.right, with(density) { 8.dp.toPx() })
        assertNear(feelsLikeMetric.left - firstDivider.right, with(density) { 8.dp.toPx() })
        assertNear(secondDivider.left - feelsLikeMetric.right, with(density) { 8.dp.toPx() })
        assertNear(weatherMetric.left - secondDivider.right, with(density) { 8.dp.toPx() })
        assertNear(humidityLabel.left, with(density) { 62.dp.toPx() })
        assertNear(feelsLikeLabel.left, with(density) { 159.dp.toPx() })
        assertNear(weatherLabel.left, with(density) { 266.dp.toPx() })
        assertTextHeight("습도", expectedHeight = 14.sp)
        assertTextHeight("체감온도", expectedHeight = 14.sp)
        assertTextHeight("날씨", expectedHeight = 14.sp)
    }

    private fun assertBounds(
        bounds: androidx.compose.ui.geometry.Rect,
        left: Dp,
        width: Dp,
    ) {
        val density = composeRule.density
        assertNear(bounds.left, with(density) { left.toPx() })
        assertNear(bounds.width, with(density) { width.toPx() })
    }

    private fun assertMetricLabelsAreStacked() {
        val labelBounds =
            listOf("습도", "체감온도", "날씨").map { label ->
                composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            }

        assertTrue(labelBounds[0].center.y < labelBounds[1].center.y)
        assertTrue(labelBounds[1].center.y < labelBounds[2].center.y)
    }

    private fun assertTextHeight(
        text: String,
        expectedHeight: TextUnit,
    ) {
        val textBounds = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        assertNear(textBounds.height, with(composeRule.density) { expectedHeight.toPx() })
    }

    private fun assertNear(
        actual: Float,
        expected: Float,
    ) {
        val tolerance = with(composeRule.density) { 1.dp.toPx() }
        assertTrue(abs(actual - expected) <= tolerance)
    }
}
