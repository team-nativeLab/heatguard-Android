package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import javax.inject.Inject

/** 기록 상세 화면이 표시할 기록 한 건(사진 URL 포함)을 조회한다. */
class GetRecordDetailUseCase @Inject constructor(
    private val recordHistoryRepository: RecordHistoryRepository,
) {
    suspend operator fun invoke(recordId: String): ApiResult<RecordHistoryEntry> =
        recordHistoryRepository.getRecordDetail(recordId)
}
