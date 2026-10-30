package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistRoundItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundDetailResponseDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistRoundsResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TeamChecklistApiService {
    @GET("api/v1/team/checklist")
    suspend fun getChecklist(): ApiEnvelope<TeamChecklistResponseDto>

    @PUT("api/v1/team/checklist/items/{itemId}")
    suspend fun setItemChecked(
        @Path("itemId") itemId: String,
        @Body request: UpdateChecklistItemRequestDto,
    ): ApiEnvelope<ChecklistItemStatusDto>

    @GET("api/v1/team/checklist/rounds")
    suspend fun getRounds(
        @Query("date") date: String,
    ): ApiEnvelope<TeamChecklistRoundsResponseDto>

    @GET("api/v1/team/checklist/rounds/{roundId}")
    suspend fun getRound(
        @Path(value = "roundId", encoded = true) encodedRoundId: String,
        @Query("date") date: String,
    ): ApiEnvelope<TeamChecklistRoundDetailResponseDto>

    @PUT("api/v1/team/checklist/rounds/{roundId}/items/{itemId}")
    suspend fun setRoundItemChecked(
        @Path(value = "roundId", encoded = true) encodedRoundId: String,
        @Path(value = "itemId", encoded = true) encodedItemId: String,
        @Body request: UpdateChecklistRoundItemRequestDto,
    ): ApiEnvelope<ChecklistRoundItemStatusDto>
}
