package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDate
import javax.inject.Inject

/** 기록 내역 달력·주간 스트립에 표시할 날짜별 기록 유무를 조회한다.
 * 날짜마다 첫 페이지 한 번이면 기록 유무를 알 수 있으므로 날짜당 요청은 한 건이다. */
class GetRecordDayMarkersUseCase
    @Inject
    constructor(
        private val recordHistoryRepository: RecordHistoryRepository,
    ) {
        /** [dates]의 날짜별 기록 유무를 돌려준다. 조회에 실패한 날짜는 결과에 넣지 않아 '기록 없음'으로 오인되지 않게 한다. */
        suspend operator fun invoke(dates: List<LocalDate>): Map<LocalDate, Boolean> {
            val dayResults =
                coroutineScope {
                    // 서버 요청이 한꺼번에 몰리지 않도록 동시에 나가는 날짜 요청 수를 제한한다.
                    val requestPermits = Semaphore(MAX_CONCURRENT_REQUESTS)
                    dates
                        .map { date ->
                            async {
                                requestPermits.withPermit {
                                    date to fetchHasRecord(date)
                                }
                            }
                        }.awaitAll()
                }

            return dayResults
                .mapNotNull { (date, hasRecord) ->
                    hasRecord?.let { recordExists -> date to recordExists }
                }.toMap()
        }

        /** 하루 첫 페이지로 기록 유무를 판단한다. 실패하면 null이며, 취소는 다시 던져 호출자가 멈추게 한다. */
        private suspend fun fetchHasRecord(date: LocalDate): Boolean? =
            try {
                when (val pageResult = recordHistoryRepository.getRecordPage(date = date, cursor = null)) {
                    is ApiResult.Success -> {
                        val recordPage = pageResult.value
                        recordPage.entries.isNotEmpty() || recordPage.nextCursor != null
                    }

                    is ApiResult.Failure -> {
                        null
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                null
            }

        companion object {
            const val MAX_CONCURRENT_REQUESTS = 7
        }
    }
