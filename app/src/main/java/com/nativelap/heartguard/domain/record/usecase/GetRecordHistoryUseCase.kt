package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.RecordHistoryEntry
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** 기록 내역 화면의 기간([startDate]~[endDate], 양 끝 포함) 기록을 최신 측정 시각 순으로 모아 준다.
 * 서버가 하루 단위 조회만 지원하므로 날짜별로 동시에 요청하고, 하나라도 실패하면 첫 실패를 돌려준다.
 * 요청 수가 과도해지지 않도록 기간은 [MAX_RANGE_DAYS]일로 제한하며, 화면도 같은 한도로 기간 선택을 막는다. */
class GetRecordHistoryUseCase @Inject constructor(
    private val recordHistoryRepository: RecordHistoryRepository,
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
    ): ApiResult<List<RecordHistoryEntry>> {
        require(!endDate.isBefore(startDate)) {
            "기간 종료일이 시작일보다 앞설 수 없습니다."
        }
        require(startDate.plusDays(MAX_RANGE_DAYS - 1L) >= endDate) {
            "기록 조회 기간은 최대 ${MAX_RANGE_DAYS}일입니다."
        }

        val requestDates = generateSequence(startDate) { requestDate -> requestDate.plusDays(1) }
            .takeWhile { requestDate -> !requestDate.isAfter(endDate) }
            .toList()
        val dailyResults = coroutineScope {
            requestDates
                .map { requestDate ->
                    async {
                        recordHistoryRepository.getRecordsOfDate(requestDate)
                    }
                }
                .awaitAll()
        }

        val collectedEntries = mutableListOf<RecordHistoryEntry>()
        dailyResults.forEach { dailyResult ->
            when (dailyResult) {
                is ApiResult.Success -> collectedEntries += dailyResult.value
                is ApiResult.Failure -> return dailyResult
            }
        }

        return ApiResult.Success(
            collectedEntries.sortedByDescending { recordEntry -> recordEntry.measuredAt },
        )
    }

    companion object {
        const val MAX_RANGE_DAYS = 31
    }
}
