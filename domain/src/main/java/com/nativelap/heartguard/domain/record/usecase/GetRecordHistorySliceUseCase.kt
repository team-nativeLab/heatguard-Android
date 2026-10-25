package com.nativelap.heartguard.domain.record.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.RecordHistoryPosition
import com.nativelap.heartguard.domain.record.model.RecordHistorySlice
import com.nativelap.heartguard.domain.record.repository.RecordHistoryRepository
import java.time.LocalDate
import javax.inject.Inject

/** 기록 내역 화면이 스크롤할 때 기간 기록을 최신 날짜부터 한 페이지씩 이어 받는다.
 * 서버가 하루 단위로만 조회하므로 [position] 날짜의 커서를 다 쓰면 하루 전 날짜로 넘어가고,
 * 기록이 없는 날은 건너뛰어 화면에 보여줄 기록이 생기거나 [startDate]에 닿을 때까지 이어서 요청한다. */
class GetRecordHistorySliceUseCase
    @Inject
    constructor(
        private val recordHistoryRepository: RecordHistoryRepository,
    ) {
        suspend operator fun invoke(
            startDate: LocalDate,
            position: RecordHistoryPosition,
        ): ApiResult<RecordHistorySlice> {
            var requestPosition = position
            while (true) {
                val pageResult =
                    recordHistoryRepository.getRecordPage(
                        date = requestPosition.date,
                        cursor = requestPosition.cursor,
                    )
                val recordPage =
                    when (pageResult) {
                        is ApiResult.Success -> pageResult.value
                        is ApiResult.Failure -> return pageResult
                    }
                val nextPosition =
                    nextPositionAfter(
                        currentDate = requestPosition.date,
                        nextCursor = recordPage.nextCursor,
                        startDate = startDate,
                    )
                // 같은 날의 다음 페이지가 남았거나 받은 기록이 있으면 여기서 돌려주고, 빈 날만 이어서 건너뛴다.
                if (recordPage.entries.isNotEmpty() || nextPosition == null || nextPosition.cursor != null) {
                    return ApiResult.Success(
                        RecordHistorySlice(
                            entries = recordPage.entries.sortedByDescending { recordEntry -> recordEntry.measuredAt },
                            nextPosition = nextPosition,
                        ),
                    )
                }
                requestPosition = nextPosition
            }
        }

        private fun nextPositionAfter(
            currentDate: LocalDate,
            nextCursor: String?,
            startDate: LocalDate,
        ): RecordHistoryPosition? =
            when {
                nextCursor != null -> {
                    RecordHistoryPosition(
                        date = currentDate,
                        cursor = nextCursor,
                    )
                }

                currentDate.isAfter(startDate) -> {
                    RecordHistoryPosition(
                        date = currentDate.minusDays(1),
                        cursor = null,
                    )
                }

                else -> {
                    null
                }
            }
    }
