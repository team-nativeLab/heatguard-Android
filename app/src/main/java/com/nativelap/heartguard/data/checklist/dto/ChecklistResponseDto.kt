package com.nativelap.heartguard.data.checklist.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ChecklistResponseDto(
    @SerialName("date")
    val date: String,
    @SerialName("items")
    val items: List<ChecklistItemDto>,
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
data class ChecklistItemUpdateRequestDto(
    @SerialName("checked")
    val checked: Boolean,
)

@Serializable
data class ChecklistItemUpdateResponseDto(
    @SerialName("itemId")
    val itemId: String,
    @SerialName("checked")
    val checked: Boolean,
    @SerialName("checkedAt")
    val checkedAt: String? = null,
)
