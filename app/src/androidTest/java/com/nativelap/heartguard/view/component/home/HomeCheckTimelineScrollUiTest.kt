package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeCheckTimelineScrollUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun twentyFourCheckTimesScrollFromCurrentTimeToLastTimeWithoutOpeningChecklist() {
        var checklistClickCount = 0
        val timelineItems =
            (0 until TIMELINE_ITEM_COUNT).map { hour ->
                CheckTimelineItem(
                    timeLabel = "${hour.toString().padStart(2, '0')}시",
                    isCompleted = hour < CURRENT_TIMELINE_INDEX,
                    isCurrent = hour == CURRENT_TIMELINE_INDEX,
                )
            }
        setTimeline(
            width = TIMELINE_VIEWPORT_WIDTH,
            items = timelineItems,
            onChecklistClick = { checklistClickCount++ },
        )

        composeRule
            .onNode(hasScrollAction(), useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).assertIsDisplayed()
        composeRule
            .onNodeWithText(timelineItems[CURRENT_TIMELINE_INDEX].timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(timelineItems.first().timeLabel, useUnmergedTree = true)
            .assertIsNotDisplayed()

        composeRule
            .onNode(hasScrollAction(), useUnmergedTree = true)
            .performTouchInput { swipeLeft() }

        composeRule
            .onNodeWithText(timelineItems.last().timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()
        assertEquals(0, checklistClickCount)
    }

    @Test
    fun timelineWithoutCurrentTimeStartsAtLastCheckTime() {
        val timelineItems =
            (0 until TIMELINE_ITEM_COUNT).map { hour ->
                CheckTimelineItem(
                    timeLabel = "${hour.toString().padStart(2, '0')}시",
                    isCompleted = false,
                )
            }
        setTimeline(
            width = TIMELINE_VIEWPORT_WIDTH,
            items = timelineItems,
        )

        composeRule
            .onNodeWithText(timelineItems.last().timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(timelineItems.first().timeLabel, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun changedTimeListResetsToCurrentButCurrentStatusUpdatesKeepManualScroll() {
        val initialTimelineItems = createTimelineItems(currentIndex = CURRENT_TIMELINE_INDEX)
        val timelineItemsState = mutableStateOf(initialTimelineItems)
        setTimelineWithState(timelineItemsState)

        composeRule
            .onNodeWithText(initialTimelineItems[CURRENT_TIMELINE_INDEX].timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule
            .onNode(hasScrollAction(), useUnmergedTree = true)
            .performTouchInput { swipeLeft() }
        composeRule
            .onNodeWithText(initialTimelineItems.last().timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()

        val updatedCurrentItems =
            initialTimelineItems.mapIndexed { index, timelineItem ->
                timelineItem.copy(isCurrent = index == 3)
            }
        composeRule.runOnIdle {
            timelineItemsState.value = updatedCurrentItems
        }
        composeRule
            .onNodeWithText(updatedCurrentItems.last().timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(updatedCurrentItems.first().timeLabel, useUnmergedTree = true)
            .assertIsNotDisplayed()

        val replacementTimelineItems = createTimelineItems(currentIndex = 5, labelPrefix = "새")
        composeRule.runOnIdle {
            timelineItemsState.value = replacementTimelineItems
        }
        composeRule
            .onNodeWithText(replacementTimelineItems[5].timeLabel, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(replacementTimelineItems.last().timeLabel, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun smallTimelineKeepsEveryTimeVisibleAndCardOpensChecklist() {
        var checklistClickCount = 0
        val timelineItems =
            listOf(
                CheckTimelineItem("08시", isCompleted = true),
                CheckTimelineItem("10시", isCompleted = false, isCurrent = true),
                CheckTimelineItem("12시", isCompleted = false),
            )
        setTimeline(
            width = TIMELINE_VIEWPORT_WIDTH,
            items = timelineItems,
            onChecklistClick = { checklistClickCount++ },
        )

        timelineItems.forEach { timelineItem ->
            composeRule
                .onNodeWithText(timelineItem.timeLabel, useUnmergedTree = true)
                .assertIsDisplayed()
        }
        composeRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.home_checklist_card_accessibility_format,
                    TIMELINE_TITLE,
                    NEXT_CHECK_DESCRIPTION,
                ),
            ).assertHasClickAction()
            .performClick()

        assertEquals(1, checklistClickCount)
    }

    @Test
    fun emptyTimelineKeepsChecklistCardAccessible() {
        var checklistClickCount = 0
        setTimeline(
            width = TIMELINE_VIEWPORT_WIDTH,
            items = emptyList(),
            onChecklistClick = { checklistClickCount++ },
        )

        composeRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.home_checklist_card_accessibility_format,
                    TIMELINE_TITLE,
                    NEXT_CHECK_DESCRIPTION,
                ),
            ).assertHasClickAction()
            .performClick()

        assertEquals(1, checklistClickCount)
    }

    private fun setTimeline(
        width: Dp,
        items: List<CheckTimelineItem>,
        onChecklistClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            HeartGuardTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeCheckTimeline(
                        title = TIMELINE_TITLE,
                        nextCheckDescription = NEXT_CHECK_DESCRIPTION,
                        items = items,
                        onClick = onChecklistClick,
                        modifier = Modifier.width(width),
                    )
                }
            }
        }
    }

    private fun setTimelineWithState(items: MutableState<List<CheckTimelineItem>>) {
        composeRule.setContent {
            HeartGuardTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeCheckTimeline(
                        title = TIMELINE_TITLE,
                        nextCheckDescription = NEXT_CHECK_DESCRIPTION,
                        items = items.value,
                        onClick = {},
                        modifier = Modifier.width(TIMELINE_VIEWPORT_WIDTH),
                    )
                }
            }
        }
    }

    private fun createTimelineItems(
        currentIndex: Int,
        labelPrefix: String = "",
    ): List<CheckTimelineItem> =
        (0 until TIMELINE_ITEM_COUNT).map { hour ->
            CheckTimelineItem(
                timeLabel = "$labelPrefix${hour.toString().padStart(2, '0')}시",
                isCompleted = hour < currentIndex,
                isCurrent = hour == currentIndex,
            )
        }

    private companion object {
        const val CURRENT_TIMELINE_INDEX = 12
        const val TIMELINE_ITEM_COUNT = 24
        const val TIMELINE_TITLE = "오늘 체크 시간"
        const val NEXT_CHECK_DESCRIPTION = "다음 체크까지 20분 · 12:00 예정"
        val TIMELINE_VIEWPORT_WIDTH = 320.dp
    }
}
