package com.nativelap.heartguard.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.usecase.GetTodayChecklistUseCase
import com.nativelap.heartguard.domain.checklist.usecase.SetChecklistItemCheckedUseCase
import com.nativelap.heartguard.domain.site.usecase.GetTeamSiteOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 홈 화면 진입 시 팀 현장페이지(GET /api/v1/t/{teamToken}) 정보를 불러온다. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTeamSiteOverviewUseCase: GetTeamSiteOverviewUseCase,
    private val getTodayChecklistUseCase: GetTodayChecklistUseCase,
    private val setChecklistItemCheckedUseCase: SetChecklistItemCheckedUseCase,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private val mutableChecklistUiState = MutableStateFlow<HomeChecklistUiState>(HomeChecklistUiState.Loading)
    val checklistUiState: StateFlow<HomeChecklistUiState> = mutableChecklistUiState.asStateFlow()

    init {
        loadTeamSiteOverview()
        loadTodayChecklist()
    }

    fun loadTeamSiteOverview() {
        viewModelScope.launch {
            mutableUiState.value = HomeUiState.Loading
            mutableUiState.value = when (val result = getTeamSiteOverviewUseCase()) {
                is ApiResult.Success -> HomeUiState.Success(result.value)
                is ApiResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }

    fun loadTodayChecklist() {
        viewModelScope.launch {
            mutableChecklistUiState.value = HomeChecklistUiState.Loading
            mutableChecklistUiState.value = when (val result = getTodayChecklistUseCase()) {
                is ApiResult.Success -> HomeChecklistUiState.Success(
                    date = result.value.date,
                    items = result.value.items,
                )

                is ApiResult.Failure -> HomeChecklistUiState.Error(result.error)
            }
        }
    }

    fun setChecklistItemChecked(itemId: String, checked: Boolean) {
        val currentState = mutableChecklistUiState.value as? HomeChecklistUiState.Success ?: return
        if (itemId !in currentState.items.map { it.itemId } || itemId in currentState.pendingItemIds) {
            return
        }

        mutableChecklistUiState.value = currentState.copy(
            pendingItemIds = currentState.pendingItemIds + itemId,
            updateError = null,
        )
        viewModelScope.launch {
            val result = setChecklistItemCheckedUseCase(itemId, checked)
            val latestState = mutableChecklistUiState.value as? HomeChecklistUiState.Success ?: return@launch
            when (result) {
                is ApiResult.Success -> {
                    val itemUpdate = result.value
                    val updatedItems = if (itemUpdate.itemId == itemId) {
                        latestState.items.map { item ->
                            if (item.itemId == itemId) {
                                item.copy(
                                    checked = itemUpdate.checked,
                                    checkedAt = itemUpdate.checkedAt,
                                )
                            } else {
                                item
                            }
                        }
                    } else {
                        latestState.items
                    }
                    mutableChecklistUiState.value = latestState.copy(
                        items = updatedItems,
                        pendingItemIds = latestState.pendingItemIds - itemId,
                    )
                    if (itemUpdate.itemId != itemId) {
                        loadTodayChecklist()
                    }
                }

                is ApiResult.Failure -> {
                    mutableChecklistUiState.value = latestState.copy(
                        pendingItemIds = latestState.pendingItemIds - itemId,
                        updateError = result.error,
                    )
                }
            }
        }
    }
}
