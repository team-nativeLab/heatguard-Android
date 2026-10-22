package com.nativelap.heartguard.viewmodel.checklist

import androidx.lifecycle.ViewModel
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
internal class ChecklistDemoViewModel
    @Inject
    constructor() : ViewModel() {
        private val mutableUiState = MutableStateFlow(ChecklistDemoUiState())
        val uiState = mutableUiState.asStateFlow()

        fun startDemo() {
            mutableUiState.value = ChecklistDemoUiState()
        }

        fun checkSchedule(state: ChecklistDemoUiState = mutableUiState.value): CheckSchedule {
            return CheckSchedule(
                checkTimes = state.rounds.map(ChecklistDemoRoundUiModel::time),
                nextCheckTime = state.currentTime,
                minutesUntilNextCheck = null,
                completedCheckTimes =
                    state.rounds
                        .filter(ChecklistDemoRoundUiModel::isComplete)
                        .map(ChecklistDemoRoundUiModel::time)
                        .toSet(),
            )
        }

        fun checklistUiState(
            time: LocalTime,
            items: List<ChecklistItemUiModel>,
            state: ChecklistDemoUiState = mutableUiState.value,
        ): ChecklistUiState {
            val selectedRound = state.rounds.firstOrNull { round -> round.time == time }
            return ChecklistUiState(
                date = CHECKLIST_DEMO_DATE,
                items = items,
                periods =
                    state.rounds.map { round ->
                        ChecklistPeriodUiModel(
                            time = round.time,
                            status = round.status(state.currentTime),
                        )
                    },
                hasLoaded = selectedRound != null,
                hasLoadError = selectedRound == null,
            )
        }

        fun setItemChecked(
            time: LocalTime,
            itemId: String,
            isChecked: Boolean,
        ) {
            val state = mutableUiState.value
            mutableUiState.value =
                state.copy(
                    rounds =
                        state.rounds.map { round ->
                            if (round.time != time || round.isComplete) {
                                round
                            } else {
                                round.copy(
                                    items =
                                        round.items.map { checklistItem ->
                                            if (checklistItem.itemId == itemId) {
                                                checklistItem.copy(isChecked = isChecked)
                                            } else {
                                                checklistItem
                                            }
                                        },
                                )
                            }
                        },
                )
        }

        fun completeRound(time: LocalTime) {
            val state = mutableUiState.value
            val selectedRound = state.rounds.firstOrNull { round -> round.time == time } ?: return
            if (selectedRound.items.isEmpty() || selectedRound.items.any { item -> !item.isChecked }) return

            val updatedRounds =
                state.rounds.map { round ->
                    if (round.time == time) {
                        round.copy(isComplete = true)
                    } else {
                        round
                    }
                }
            val nextCurrentTime =
                if (state.currentTime == time) {
                    updatedRounds.firstOrNull { round ->
                        !round.isComplete && round.time.isAfter(time)
                    }?.time
                } else {
                    state.currentTime
                }
            mutableUiState.value =
                state.copy(
                    rounds = updatedRounds,
                    currentTime = nextCurrentTime,
                )
        }
    }

internal data class ChecklistDemoUiState(
    val rounds: List<ChecklistDemoRoundUiModel> = createInitialDemoRounds(),
    val currentTime: LocalTime? = LocalTime.of(20, 0),
)

internal data class ChecklistDemoRoundUiModel(
    val time: LocalTime,
    val items: List<ChecklistDemoItemUiModel>,
    val isComplete: Boolean,
) {
    fun status(currentTime: LocalTime?): ChecklistPeriodStatus =
        when {
            isComplete -> ChecklistPeriodStatus.COMPLETED
            time == currentTime -> ChecklistPeriodStatus.CURRENT
            currentTime == null || time.isBefore(currentTime) -> ChecklistPeriodStatus.WARNING
            else -> ChecklistPeriodStatus.UPCOMING
        }
}

internal data class ChecklistDemoItemUiModel(
    val itemId: String,
    val titleResourceId: Int,
    val isChecked: Boolean,
)

internal val CHECKLIST_DEMO_DATE: LocalDate = LocalDate.of(2026, 9, 27)

internal val CHECKLIST_DEMO_ITEM_TITLE_RESOURCE_IDS =
    listOf(
        R.string.checklist_demo_item_water,
        R.string.checklist_demo_item_shade,
        R.string.checklist_demo_item_health,
        R.string.checklist_demo_item_work_pause,
        R.string.checklist_demo_item_rest,
        R.string.checklist_demo_item_first_aid,
    )

private fun createInitialDemoRounds(): List<ChecklistDemoRoundUiModel> =
    listOf(
        demoRound(LocalTime.of(8, 0), checkedCount = 6, isComplete = true),
        demoRound(LocalTime.of(10, 0), checkedCount = 6, isComplete = true),
        demoRound(LocalTime.of(12, 0), checkedCount = 3, isComplete = false),
        demoRound(LocalTime.of(14, 0), checkedCount = 6, isComplete = true),
        demoRound(LocalTime.of(16, 0), checkedCount = 0, isComplete = false),
        demoRound(LocalTime.of(18, 0), checkedCount = 6, isComplete = true),
        demoRound(LocalTime.of(20, 0), checkedCount = 3, isComplete = false),
        demoRound(LocalTime.of(22, 0), checkedCount = 0, isComplete = false),
    )

private fun demoRound(
    time: LocalTime,
    checkedCount: Int,
    isComplete: Boolean,
): ChecklistDemoRoundUiModel {
    return ChecklistDemoRoundUiModel(
        time = time,
        items =
            CHECKLIST_DEMO_ITEM_TITLE_RESOURCE_IDS.mapIndexed { index, titleResourceId ->
                ChecklistDemoItemUiModel(
                    itemId = "demo_${time.hour}_$index",
                    titleResourceId = titleResourceId,
                    isChecked = index < checkedCount,
                )
            },
        isComplete = isComplete,
    )
}
