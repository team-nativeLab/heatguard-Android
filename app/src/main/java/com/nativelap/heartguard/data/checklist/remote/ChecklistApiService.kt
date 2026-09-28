package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.ApiEnvelope
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateRequestDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistItemUpdateResponseDto
import com.nativelap.heartguard.data.checklist.dto.ChecklistResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChecklistApiService {
    @GET("api/v1/team/checklist")
    suspend fun getTodayChecklist(): ApiEnvelope<ChecklistResponseDto>

    @PUT("api/v1/team/checklist/items/{itemId}")
    suspend fun setItemChecked(
        @Path("itemId") itemId: String,
        @Body request: ChecklistItemUpdateRequestDto,
    ): ApiEnvelope<ChecklistItemUpdateResponseDto>
}
