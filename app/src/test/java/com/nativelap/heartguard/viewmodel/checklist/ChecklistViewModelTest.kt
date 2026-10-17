package com.nativelap.heartguard.viewmodel.checklist

import com.nativelap.heartguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistViewModelTest {
    private val viewModel = ChecklistViewModel()

    @Test
    fun `체크리스트를 열면 세 항목이 완료된 상태로 시작한다`() {
        assertEquals(3, viewModel.uiState.value.completedCount)
        assertFalse(viewModel.uiState.value.isComplete)
    }

    @Test
    fun `목표 체크 상태를 반복 적용해도 완료 개수는 뒤집히지 않는다`() {
        viewModel.setChecklistItemChecked(
            titleResId = R.string.checklist_item_health,
            isChecked = true,
        )
        viewModel.setChecklistItemChecked(
            titleResId = R.string.checklist_item_health,
            isChecked = true,
        )

        assertEquals(4, viewModel.uiState.value.completedCount)
    }

    @Test
    fun `모든 항목을 완료한 뒤 데모 초기화하면 세 항목 완료 상태로 돌아간다`() {
        viewModel.uiState.value.items.forEach { item ->
            viewModel.setChecklistItemChecked(
                titleResId = item.titleResId,
                isChecked = true,
            )
        }

        assertTrue(viewModel.uiState.value.isComplete)

        viewModel.resetDemoState()

        assertEquals(3, viewModel.uiState.value.completedCount)
        assertFalse(viewModel.uiState.value.isComplete)
    }
}
