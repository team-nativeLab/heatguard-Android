package com.nativelap.heartguard.data.checklist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamChecklistResponseDto(
    @SerialName("date")
    val date: String,
    @SerialName("items")
    val items: List<ChecklistItemDto> = emptyList(),
)

@Serializable
data class ChecklistItemDto(
    @SerialName("itemId")
    val itemId: String,
    @SerialName("text")
    val text: String,
    @SerialName("sortOrder")
    val sortOrder: Int,
    @SerialName("checked")
    val checked: Boolean,
    @SerialName("checkedAt")
    val checkedAt: String? = null,
)

@Serializable
data class UpdateChecklistItemRequestDto(
    @SerialName("checked")
    val checked: Boolean,
)

@Serializable
data class ChecklistItemStatusDto(
    @SerialName("itemId")
    val itemId: String,
    @SerialName("checked")
    val checked: Boolean,
    @SerialName("checkedAt")
    val checkedAt: String? = null,
)
