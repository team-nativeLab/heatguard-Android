package com.nativelap.heartguard.view.screen.history

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter
import com.nativelap.heartguard.viewmodel.history.RecordHistoryLoadState
import com.nativelap.heartguard.viewmodel.history.RecordHistoryScreenEvent
import com.nativelap.heartguard.viewmodel.history.RecordHistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class RecordHistoryTemporaryDraftTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun temporaryDraftRowShowsAndResumesWhenSelected() {
        var receivedEvent: RecordHistoryScreenEvent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                RecordHistoryScreen(
                    uiState = historyUiState(),
                    temporaryDraftType = RecordType.TEMPERATURE,
                    temporaryDraftSavedAt = OffsetDateTime.parse("2026-09-27T16:00:00+09:00"),
                    onEvent = { event -> receivedEvent = event },
                )
            }
        }

        composeTestRule.onNodeWithText("임시저장").fetchSemanticsNode()
        composeTestRule.onNodeWithText("저장되지 않았어요").fetchSemanticsNode()
        composeTestRule.onNodeWithText("온도계 기록").performClick()

        composeTestRule.runOnIdle {
            assertEquals(RecordHistoryScreenEvent.TemporaryDraftClicked, receivedEvent)
        }
    }

    @Test
    fun temporaryDraftRowRespectsSelectedTypeFilter() {
        composeTestRule.setContent {
            HeartGuardTheme {
                RecordHistoryScreen(
                    uiState = historyUiState(selectedFilter = RecordHistoryFilter.REST),
                    temporaryDraftType = RecordType.TEMPERATURE,
                    onEvent = {},
                )
            }
        }

        assertEquals(
            0,
            composeTestRule.onAllNodesWithText("임시저장").fetchSemanticsNodes().size,
        )
        composeTestRule.onNodeWithText("해당 기간에 휴식 사진 기록이 없어요").fetchSemanticsNode()
    }

    private fun historyUiState(selectedFilter: RecordHistoryFilter = RecordHistoryFilter.ALL) =
        RecordHistoryUiState(
            today = LocalDate.of(2026, 9, 27),
            startDate = LocalDate.of(2026, 9, 21),
            endDate = LocalDate.of(2026, 9, 27),
            selectedFilter = selectedFilter,
            loadState = RecordHistoryLoadState.Loaded(emptyList()),
        )
}
