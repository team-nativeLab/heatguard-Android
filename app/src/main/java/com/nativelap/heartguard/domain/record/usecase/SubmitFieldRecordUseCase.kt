package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import java.time.OffsetDateTime
import javax.inject.Inject

class SubmitFieldRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    suspend operator fun invoke(
        type: FieldRecordType,
        photoKeys: List<String>,
        measuredAt: OffsetDateTime,
        temperature: Double?,
        humidity: Double?,
        memo: String?,
    ): ApiResult<FieldRecord> = recordRepository.submitFieldRecord(
        type = type,
        photoKeys = photoKeys,
        measuredAt = measuredAt,
        temperature = temperature,
        humidity = humidity,
        memo = memo,
    )
}
