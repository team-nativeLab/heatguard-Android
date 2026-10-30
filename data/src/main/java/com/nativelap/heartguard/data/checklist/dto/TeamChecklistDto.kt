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

@Serializable
data class TeamChecklistRoundsResponseDto(
    @SerialName("date")
    val date: String,
    @SerialName("teamId")
    val teamId: String,
    @SerialName("summary")
    val summary: ChecklistRoundSummaryDto,
    @SerialName("items")
    val items: List<ChecklistRoundSummaryItemDto> = emptyList(),
)

@Serializable
data class ChecklistRoundSummaryDto(
    @SerialName("totalRounds")
    val totalRounds: Int,
    @SerialName("completedRounds")
    val completedRounds: Int,
    @SerialName("missedRounds")
    val missedRounds: Int,
    @SerialName("remainingRounds")
    val remainingRounds: Int,
    @SerialName("progressPercent")
    val progressPercent: Int,
)

@Serializable
data class ChecklistRoundSummaryItemDto(
    @SerialName("roundId")
    val roundId: String,
    @SerialName("startsAt")
    val startsAt: String,
    @SerialName("endsAt")
    val endsAt: String,
    @SerialName("status")
    val status: String,
    @SerialName("totalItems")
    val totalItems: Int,
    @SerialName("checkedItems")
    val checkedItems: Int,
    @SerialName("progressPercent")
    val progressPercent: Int,
    @SerialName("completedAt")
    val completedAt: String? = null,
)

@Serializable
data class TeamChecklistRoundDetailResponseDto(
    @SerialName("date")
    val date: String,
    @SerialName("teamId")
    val teamId: String,
    @SerialName("roundId")
    val roundId: String,
    @SerialName("startsAt")
    val startsAt: String,
    @SerialName("endsAt")
    val endsAt: String,
    @SerialName("status")
    val status: String,
    @SerialName("totalItems")
    val totalItems: Int,
    @SerialName("checkedItems")
    val checkedItems: Int,
    @SerialName("progressPercent")
    val progressPercent: Int,
    @SerialName("completedAt")
    val completedAt: String? = null,
    @SerialName("items")
    val items: List<ChecklistRoundItemDto> = emptyList(),
)

@Serializable
data class ChecklistRoundItemDto(
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
data class UpdateChecklistRoundItemRequestDto(
    @SerialName("checked")
    val checked: Boolean,
)

@Serializable
data class ChecklistRoundItemStatusDto(
    @SerialName("date")
    val date: String,
    @SerialName("roundId")
    val roundId: String,
    @SerialName("itemId")
    val itemId: String,
    @SerialName("checked")
    val checked: Boolean,
    @SerialName("checkedAt")
    val checkedAt: String? = null,
)
