package com.nativelap.heartguard.viewmodel.checklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class ChecklistDemoViewModelTest {
    private val viewModel = ChecklistDemoViewModel()

    @Test
    fun `초기 데모는 여덟 회차 중 네 회차 완료와 20시 3 slash 6 상태를 제공한다`() {
        val demoState = viewModel.uiState.value
        val schedule = viewModel.checkSchedule(demoState)
        val twentyOClockState = checklistUiState(LocalTime.of(20, 0), demoState)

        assertEquals(8, schedule.checkTimes.size)
        assertEquals(4, schedule.completedCheckTimes.size)
        assertEquals(LocalTime.of(20, 0), schedule.nextCheckTime)
        assertEquals(3, twentyOClockState.completedCount)
        assertEquals(6, twentyOClockState.items.size)
        assertEquals(ChecklistPeriodStatus.WARNING, twentyOClockState.periods[2].status)
        assertEquals(ChecklistPeriodStatus.CURRENT, twentyOClockState.periods[6].status)
        assertEquals(ChecklistPeriodStatus.UPCOMING, twentyOClockState.periods[7].status)
    }

    @Test
    fun `20시 항목을 모두 체크하고 완료하면 목록 회차 요약이 갱신된다`() {
        val twentyOClock = LocalTime.of(20, 0)
        val currentRound = checklistUiState(twentyOClock)
        currentRound.items
            .filterNot(ChecklistItemUiModel::isChecked)
            .forEach { item ->
                viewModel.setItemChecked(twentyOClock, item.itemId, true)
            }

        val completedRound = checklistUiState(twentyOClock)
        assertTrue(completedRound.isComplete)

        viewModel.completeRound(twentyOClock)

        val updatedSchedule = viewModel.checkSchedule()
        assertEquals(5, updatedSchedule.completedCheckTimes.size)
        assertEquals(LocalTime.of(22, 0), updatedSchedule.nextCheckTime)
        assertEquals(
            ChecklistPeriodStatus.COMPLETED,
            checklistUiState(twentyOClock).periods[6].status,
        )
    }

    @Test
    fun `회차 진행 상태는 다른 회차 항목과 섞이지 않는다`() {
        val twelveOClock = LocalTime.of(12, 0)
        val twentyOClock = LocalTime.of(20, 0)
        val twelveOClockItem = checklistUiState(twelveOClock).items[3]

        viewModel.setItemChecked(twelveOClock, twelveOClockItem.itemId, true)

        assertTrue(checklistUiState(twelveOClock).items[3].isChecked)
        assertFalse(checklistUiState(twentyOClock).items[3].isChecked)
    }

    @Test
    fun `데모를 다시 시작하면 초기 요약과 회차 진행 상태로 되돌아간다`() {
        val twentyOClock = LocalTime.of(20, 0)
        val item = checklistUiState(twentyOClock).items[3]
        viewModel.setItemChecked(twentyOClock, item.itemId, true)

        viewModel.startDemo()

        val resetState = checklistUiState(twentyOClock)
        assertEquals(3, resetState.completedCount)
        assertEquals(4, viewModel.checkSchedule().completedCheckTimes.size)
    }

    private fun checklistUiState(
        time: LocalTime,
        demoState: ChecklistDemoUiState = viewModel.uiState.value,
    ): ChecklistUiState {
        val items =
            demoState.rounds
                .first { round -> round.time == time }
                .items
                .map { item ->
                    ChecklistItemUiModel(
                        itemId = item.itemId,
                        title = item.titleResourceId.toString(),
                        isChecked = item.isChecked,
                    )
                }
        return viewModel.checklistUiState(time, items, demoState)
    }
}
