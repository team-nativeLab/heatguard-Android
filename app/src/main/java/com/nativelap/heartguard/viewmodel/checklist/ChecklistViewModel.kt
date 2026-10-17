package com.nativelap.heartguard.viewmodel.checklist

import androidx.lifecycle.ViewModel
import com.nativelap.heartguard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 서버 체크리스트 연동 전까지 화면 동작을 확인하기 위한 로컬 데모 상태다. */
@HiltViewModel
internal class ChecklistViewModel @Inject constructor() : ViewModel() {

    private val mutableUiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = mutableUiState.asStateFlow()

    fun resetDemoState() {
        mutableUiState.value = ChecklistUiState()
    }

    fun toggleChecklistItem(titleResId: Int) {
        mutableUiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.titleResId == titleResId) {
                        item.copy(isChecked = !item.isChecked)
                    } else {
                        item
                    }
                },
            )
        }
    }
}

internal data class ChecklistUiState(
    val items: List<ChecklistItemUiModel> = initialChecklistItems(),
    val periods: List<ChecklistPeriodUiModel> = initialChecklistPeriods(),
) {
    val completedCount: Int
        get() = items.count(ChecklistItemUiModel::isChecked)

    val isComplete: Boolean
        get() = items.isNotEmpty() && completedCount == items.size
}

internal data class ChecklistItemUiModel(
    val titleResId: Int,
    val isChecked: Boolean,
)

internal data class ChecklistPeriodUiModel(
    val hour: Int,
    val status: ChecklistPeriodStatus,
)

internal enum class ChecklistPeriodStatus {
    COMPLETED,
    WARNING,
    CURRENT,
    UPCOMING,
}

private fun initialChecklistItems(): List<ChecklistItemUiModel> = listOf(
    ChecklistItemUiModel(R.string.checklist_item_water, isChecked = true),
    ChecklistItemUiModel(R.string.checklist_item_shade, isChecked = true),
    ChecklistItemUiModel(R.string.checklist_item_health, isChecked = false),
    ChecklistItemUiModel(R.string.checklist_item_work_hours, isChecked = false),
    ChecklistItemUiModel(R.string.checklist_item_rest, isChecked = true),
    ChecklistItemUiModel(R.string.checklist_item_first_aid, isChecked = false),
)

private fun initialChecklistPeriods(): List<ChecklistPeriodUiModel> = listOf(
    ChecklistPeriodUiModel(hour = 8, status = ChecklistPeriodStatus.COMPLETED),
    ChecklistPeriodUiModel(hour = 10, status = ChecklistPeriodStatus.COMPLETED),
    ChecklistPeriodUiModel(hour = 12, status = ChecklistPeriodStatus.WARNING),
    ChecklistPeriodUiModel(hour = 14, status = ChecklistPeriodStatus.COMPLETED),
    ChecklistPeriodUiModel(hour = 16, status = ChecklistPeriodStatus.WARNING),
    ChecklistPeriodUiModel(hour = 18, status = ChecklistPeriodStatus.COMPLETED),
    ChecklistPeriodUiModel(hour = 20, status = ChecklistPeriodStatus.CURRENT),
    ChecklistPeriodUiModel(hour = 22, status = ChecklistPeriodStatus.UPCOMING),
)
