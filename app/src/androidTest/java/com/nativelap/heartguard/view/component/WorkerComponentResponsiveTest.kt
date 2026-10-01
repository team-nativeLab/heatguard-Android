package com.nativelap.heartguard.view.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.feedback.SavedRecordSummaryItem
import com.nativelap.heartguard.view.component.menu.MenuDrawerContent
import com.nativelap.heartguard.view.component.temperature.TemperatureRecordCard
import com.nativelap.heartguard.view.screen.feedback.SaveFailureScreen
import com.nativelap.heartguard.view.screen.feedback.SaveSuccessScreen
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerProfileUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkerComponentResponsiveTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun expandedSuccessAllowsFullTitleDescriptionAndLongSummary() {
        val longLocation = "서울특별시 매우 긴 현장 위치와 작업 구역 상세 정보"
        setViewport(393, 582, 2f) {
            SaveSuccessScreen(
                records = listOf(SavedRecordSummaryItem("작업 위치", longLocation, detail = "제2작업구역")),
                onCompleteClick = {},
            )
        }
        assertReadableText(string(R.string.save_record_title))
        assertReadableText(string(R.string.save_success_description))
        assertReadableText(longLocation)
        assertReadableText("제2작업구역")
        composeTestRule.onNodeWithText(string(R.string.save_complete)).assertIsDisplayed()
    }

    @Test
    fun expandedLandscapeFailureKeepsDetailsAndBothActionsReachable() {
        val errorDetail = "사진 저장에 실패했습니다. 네트워크를 확인한 후 다시 시도해 주세요."
        setViewport(582, 393, 2f) {
            SaveFailureScreen(listOf(errorDetail), {}, {})
        }
        assertReadableText(errorDetail)
        composeTestRule.onNodeWithText(string(R.string.save_retry)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.save_draft_exit)).assertIsDisplayed()
    }

    @Test
    fun shortExpandedMenuCanScrollToEveryAction() {
        setViewport(300, 240, 2f) {
            MenuDrawerContent(MenuDrawerProfileUiModel(userName = "작업자"), {})
        }
        listOf(R.string.menu_edit_profile, R.string.menu_inquiry, R.string.menu_logout, R.string.menu_withdraw)
            .forEach { titleResource ->
                composeTestRule.onNodeWithText(string(titleResource)).performScrollTo().assertIsDisplayed()
            }
    }

    @Test
    fun wideResultKeepsBodyAndActionInSameCentralColumn() {
        setViewport(840, 1180, 1f, density = 1f) {
            SaveSuccessScreen(emptyList(), {})
        }
        val titleBounds = composeTestRule.onNodeWithText(string(R.string.save_record_title))
            .fetchSemanticsNode().boundsInRoot
        val actionBounds = composeTestRule.onNodeWithText(string(R.string.save_complete))
            .fetchSemanticsNode().boundsInRoot
        assertEquals(420f, titleBounds.center.x, 1f)
        assertEquals(420f, actionBounds.center.x, 1f)
        assertTrue(titleBounds.width <= 600f)
    }

    @Test
    fun manualInputSwitchHasAccessibleNameAndSelectionState() {
        setViewport(393, 582, 2f) {
            TemperatureRecordCard(
                temperatureLabel = "온도", temperatureText = "", temperatureUnit = "°C",
                onTemperatureChange = {}, humidityLabel = "습도", humidityText = "", humidityUnit = "%",
                onHumidityChange = {}, feelsLikeLabel = "체감온도", feelsLikeText = "",
                installationLabel = "온도계 설치 안내", isManualInputEnabled = false,
                onManualInputChange = {}, checkboxContentDescription = "온도계 직접 입력",
                title = "온도계 직접 입력",
            )
        }
        composeTestRule.onNodeWithContentDescription("온도계 직접 입력").assertIsToggleable().assertIsDisplayed()
    }

    private fun assertReadableText(text: String) {
        val textNode = composeTestRule.onNodeWithText(text, substring = true, useUnmergedTree = true)
        textNode.performScrollTo().assertIsDisplayed()
        val textLayouts = mutableListOf<TextLayoutResult>()
        textNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(textLayouts) }
        assertTrue(textLayouts.isNotEmpty())
        val textLayout = textLayouts.first()
        assertFalse(textLayout.hasVisualOverflow)
        assertTrue(textNode.fetchSemanticsNode().boundsInRoot.height >= textLayout.size.height - 1f)
    }

    private fun setViewport(
        width: Int,
        height: Int,
        fontScale: Float,
        density: Float? = null,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density ?: deviceDensity, fontScale)) {
                HeartGuardTheme {
                    Box(Modifier.requiredSize(width.dp, height.dp)) { content() }
                }
            }
        }
    }

    private fun string(resource: Int): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
    }
}
