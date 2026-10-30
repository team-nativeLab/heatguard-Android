package com.nativelap.heartguard.domain.checklist.repository

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundDetail
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRounds
import java.time.LocalDate

interface TeamChecklistRepository {
    suspend fun getChecklist(): ApiResult<TeamChecklist>

    suspend fun setItemChecked(
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistItemCheckUpdate>

    suspend fun getRounds(date: LocalDate): ApiResult<TeamChecklistRounds>

    suspend fun getRound(
        roundId: String,
        date: LocalDate,
    ): ApiResult<TeamChecklistRoundDetail>

    suspend fun setRoundItemChecked(
        roundId: String,
        itemId: String,
        checked: Boolean,
    ): ApiResult<ChecklistRoundItemCheckUpdate>
}
