package com.nativelap.heartguard.data.record.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoredRecordSubmission(
    @SerialName("submissionId") val submissionId: String,
    @SerialName("type") val type: String,
    @SerialName("measuredAt") val measuredAt: String,
)
