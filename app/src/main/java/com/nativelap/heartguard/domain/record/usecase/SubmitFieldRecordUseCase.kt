package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import javax.inject.Inject

class SubmitFieldRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    suspend operator fun invoke(
        type: FieldRecordType,
        photoKeys: List<String>,
        temperature: Double?,
        humidity: Double?,
        noThermometer: Boolean,
        memo: String?,
    ): ApiResult<FieldRecord> = recordRepository.submitFieldRecord(
        type = type,
        photoKeys = photoKeys,
        temperature = temperature,
        humidity = humidity,
        noThermometer = noThermometer,
        memo = memo,
    )
}
