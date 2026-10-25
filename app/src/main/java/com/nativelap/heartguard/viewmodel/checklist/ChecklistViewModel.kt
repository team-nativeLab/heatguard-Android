package com.nativelap.heartguard.viewmodel.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistItem
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistUseCase
import com.nativelap.heartguard.domain.checklist.usecase.SetChecklistItemCheckedUseCase
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
internal class ChecklistViewModel
    @Inject
    constructor(
        private val getTeamChecklistUseCase: GetTeamChecklistUseCase,
        private val setChecklistItemCheckedUseCase: SetChecklistItemCheckedUseCase,
        private val sessionManager: SessionManager,
        private val clock: Clock,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow(ChecklistUiState())
        val uiState = mutableUiState.asStateFlow()

        private var loadJob: Job? = null
        private val saveJobs = mutableMapOf<String, Job>()
        private var requestGeneration = 0L
        private var ownedSessionGeneration = sessionManager.getSnapshot().generation

        init {
            clearStateWhenSessionEnds(sessionManager) {
                resetForSession(sessionManager.getSnapshot().generation)
            }
        }

        fun loadChecklist() {
            val requestedDate = LocalDate.now(clock)
            if (mutableUiState.value.isLoading && mutableUiState.value.date == requestedDate) return

            loadJob?.cancel()
            requestGeneration += 1L
            val request = requestGeneration
            val sessionGeneration = sessionManager.getSnapshot().generation
            ownedSessionGeneration = sessionGeneration
            val isNewDate = mutableUiState.value.date != requestedDate
            mutableUiState.value =
                mutableUiState.value.copy(
                    date = requestedDate,
                    items = if (isNewDate) emptyList() else mutableUiState.value.items,
                    hasLoaded = mutableUiState.value.hasLoaded && !isNewDate,
                    isLoading = true,
                    hasLoadError = false,
                )
            loadJob =
                viewModelScope.launch {
                    when (val result = getTeamChecklistUseCase(requestedDate)) {
                        is ApiResult.Success -> {
                            if (isCurrentRequest(request, requestedDate, sessionGeneration)) {
                                if (result.value.date != requestedDate) {
                                    mutableUiState.value =
                                        mutableUiState.value.copy(
                                            isLoading = false,
                                            hasLoadError = true,
                                        )
                                } else {
                                    mutableUiState.value =
                                        mutableUiState.value.copy(
                                            date = requestedDate,
                                            items =
                                                result.value.items.map { checklistItem ->
                                                    val serverItem = checklistItem.toUiModel()
                                                    val pendingItem =
                                                        mutableUiState.value.items.firstOrNull { currentItem ->
                                                            currentItem.itemId == checklistItem.itemId
                                                        }
                                                    if (checklistItem.itemId in mutableUiState.value.savingItemIds &&
                                                        pendingItem != null
                                                    ) {
                                                        serverItem.copy(isChecked = pendingItem.isChecked)
                                                    } else {
                                                        serverItem
                                                    }
                                                },
                                            isLoading = false,
                                            hasLoaded = true,
                                            hasLoadError = false,
                                        )
                                }
                            }
                        }

                        is ApiResult.Failure -> {
                            if (isCurrentRequest(request, requestedDate, sessionGeneration)) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        isLoading = false,
                                        hasLoadError = result.error != ApiError.SessionChanged,
                                    )
                            }
                        }
                    }
                }
        }

        fun retryLoad() {
            if (mutableUiState.value.hasLoadError && !mutableUiState.value.isLoading) {
                loadChecklist()
            }
        }

        fun refreshIfDateChanged() {
            if (mutableUiState.value.date != LocalDate.now(clock)) {
                loadChecklist()
            }
        }

        fun setCheckSchedule(schedule: CheckSchedule?) {
            val periods =
                schedule?.checkTimes.orEmpty().map { checkTime ->
                    val status =
                        when {
                            checkTime in schedule?.completedCheckTimes.orEmpty() -> {
                                ChecklistPeriodStatus.COMPLETED
                            }

                            checkTime == schedule?.nextCheckTime -> {
                                ChecklistPeriodStatus.CURRENT
                            }

                            schedule?.nextCheckTime == null || checkTime.isBefore(schedule.nextCheckTime) -> {
                                ChecklistPeriodStatus.WARNING
                            }

                            else -> {
                                ChecklistPeriodStatus.UPCOMING
                            }
                        }
                    ChecklistPeriodUiModel(time = checkTime, status = status)
                }
            mutableUiState.value = mutableUiState.value.copy(periods = periods)
        }

        fun setChecklistItemChecked(
            itemId: String,
            isChecked: Boolean,
        ) {
            val state = mutableUiState.value
            val target = state.items.firstOrNull { item -> item.itemId == itemId } ?: return
            if (target.isChecked == isChecked || saveJobs.containsKey(itemId) || state.isLoading) return

            val sessionGeneration = sessionManager.getSnapshot().generation
            val requestDate = state.date ?: return
            val originalItem = target
            mutableUiState.value =
                state.copy(
                    items =
                        state.items.map { item ->
                            if (item.itemId ==
                                itemId
                            ) {
                                item.copy(isChecked = isChecked)
                            } else {
                                item
                            }
                        },
                    savingItemIds = state.savingItemIds + itemId,
                    hasSaveError = false,
                )
            saveJobs[itemId] =
                viewModelScope.launch {
                    when (val result = setChecklistItemCheckedUseCase(itemId, isChecked)) {
                        is ApiResult.Success -> {
                            if (isCurrentSession(sessionGeneration) && mutableUiState.value.date == requestDate) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        items =
                                            mutableUiState.value.items.map { item ->
                                                if (item.itemId == itemId) {
                                                    item.copy(isChecked = result.value.checked)
                                                } else {
                                                    item
                                                }
                                            },
                                    )
                            }
                        }

                        is ApiResult.Failure -> {
                            if (isCurrentSession(sessionGeneration) && mutableUiState.value.date == requestDate) {
                                mutableUiState.value =
                                    mutableUiState.value.copy(
                                        items =
                                            mutableUiState.value.items.map { item ->
                                                if (item.itemId == itemId) originalItem else item
                                            },
                                        hasSaveError = result.error != ApiError.SessionChanged,
                                    )
                                val saveError = result.error
                                if (saveError is ApiError.Http &&
                                    saveError.statusCode in CHECKLIST_REFRESH_ERROR_CODES
                                ) {
                                    loadChecklist()
                                }
                            }
                        }
                    }
                    saveJobs.remove(itemId)
                    mutableUiState.value =
                        mutableUiState.value.copy(savingItemIds = mutableUiState.value.savingItemIds - itemId)
                }
        }

        private fun resetForSession(newSessionGeneration: Long) {
            loadJob?.cancel()
            loadJob = null
            saveJobs.values.forEach(Job::cancel)
            saveJobs.clear()
            requestGeneration += 1L
            ownedSessionGeneration = newSessionGeneration
            mutableUiState.value = ChecklistUiState()
        }

        private fun isCurrentRequest(
            request: Long,
            requestedDate: LocalDate,
            sessionGeneration: Long,
        ): Boolean =
            request == requestGeneration &&
                requestedDate == LocalDate.now(clock) &&
                isCurrentSession(sessionGeneration)

        private fun isCurrentSession(sessionGeneration: Long): Boolean =
            sessionGeneration == sessionManager.getSnapshot().generation &&
                sessionGeneration == ownedSessionGeneration

        private companion object {
            val CHECKLIST_REFRESH_ERROR_CODES = setOf(404, 409)
        }
    }

internal data class ChecklistUiState(
    val date: LocalDate? = null,
    val items: List<ChecklistItemUiModel> = emptyList(),
    val periods: List<ChecklistPeriodUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasLoadError: Boolean = false,
    val hasSaveError: Boolean = false,
    val savingItemIds: Set<String> = emptySet(),
) {
    val completedCount: Int
        get() = items.count(ChecklistItemUiModel::isChecked)

    val isComplete: Boolean
        get() = hasLoaded && items.isNotEmpty() && completedCount == items.size && savingItemIds.isEmpty()
}

internal data class ChecklistItemUiModel(
    val itemId: String,
    val title: String,
    val isChecked: Boolean,
)

internal data class ChecklistPeriodUiModel(
    val time: LocalTime,
    val status: ChecklistPeriodStatus,
)

internal enum class ChecklistPeriodStatus {
    COMPLETED,
    WARNING,
    CURRENT,
    UPCOMING,
}

private fun TeamChecklistItem.toUiModel() =
    ChecklistItemUiModel(
        itemId = itemId,
        title = text,
        isChecked = checked,
    )
