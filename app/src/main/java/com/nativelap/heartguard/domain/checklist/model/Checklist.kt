package com.nativelap.heartguard.domain.checklist.model

/** 오늘 현장 체크리스트와 서버에 저장된 각 항목의 상태다. */
data class Checklist(
    val date: String,
    val items: List<ChecklistItem>,
)

data class ChecklistItem(
    val itemId: String,
    val text: String,
    val sortOrder: Int,
    val checked: Boolean,
    val checkedAt: String?,
)

data class ChecklistItemUpdate(
    val itemId: String,
    val checked: Boolean,
    val checkedAt: String?,
)
