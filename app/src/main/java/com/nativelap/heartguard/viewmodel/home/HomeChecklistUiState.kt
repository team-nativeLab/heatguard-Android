package com.nativelap.heartguard.viewmodel.home

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.domain.checklist.model.ChecklistItem

sealed interface HomeChecklistUiState {
    data object Loading : HomeChecklistUiState

    data class Success(
        val date: String,
        val items: List<ChecklistItem>,
        val pendingItemIds: Set<String> = emptySet(),
        val updateError: ApiError? = null,
    ) : HomeChecklistUiState

    data class Error(val error: ApiError) : HomeChecklistUiState
}
