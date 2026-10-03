package com.nativelap.heartguard.domain.record.model

/** 서버 성공이 확인됐지만 로컬 표식 정리가 아직 끝나지 않은 제출이다. */
data class RecordSubmissionCleanup(val userId: String, val submissionId: String)
