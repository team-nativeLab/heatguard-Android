package com.nativelap.heartguard.domain.record.model

import java.time.OffsetDateTime

/** 서버 결과가 확정되지 않은 제출의 최소 식별 정보다. */
data class PendingRecordSubmission(
    val submissionId: String,
    val type: FieldRecordType,
    val measuredAt: OffsetDateTime,
)
