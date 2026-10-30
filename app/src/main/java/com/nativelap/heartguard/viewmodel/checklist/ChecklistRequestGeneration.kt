package com.nativelap.heartguard.viewmodel.checklist

internal class ChecklistRequestGeneration {
    private var currentGeneration = 0L

    fun begin(): Long {
        currentGeneration += 1L
        return currentGeneration
    }

    fun invalidate(): Long {
        currentGeneration += 1L
        return currentGeneration
    }

    fun isCurrent(generation: Long): Boolean = generation == currentGeneration
}

internal fun shouldStartChecklistRoundDetailLoad(
    isSaving: Boolean,
    isLoadingSameDetail: Boolean,
): Boolean = !isSaving && !isLoadingSameDetail

internal fun shouldStartChecklistRoundsLoad(isSaving: Boolean): Boolean = !isSaving
