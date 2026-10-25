package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemStatusDto
import com.nativelap.heartguard.data.checklist.dto.TeamChecklistResponseDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TeamChecklistApiService {
    @GET("api/v1/team/checklist")
    suspend fun getChecklist(
        @Query("date") date: String,
    ): ApiEnvelope<TeamChecklistResponseDto>

    @PUT("api/v1/team/checklist/items/{itemId}")
    suspend fun setItemChecked(
        @Path("itemId") itemId: String,
        @Body request: UpdateChecklistItemRequestDto,
    ): ApiEnvelope<ChecklistItemStatusDto>
}
