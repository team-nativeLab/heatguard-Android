package com.nativelap.heartguard.domain.site.model

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** 오늘의 체크 시각 목록과, 현재 시각 기준 다음 체크 시각을 정리한 모델이다.
 * [nextCheckTime]이 null이면 오늘 남은 체크가 없다는 뜻이다. */
data class CheckSchedule(
    val checkTimes: List<LocalTime>,
    val nextCheckTime: LocalTime?,
    val minutesUntilNextCheck: Long?,
    // 오늘 기록이 저장된 체크 시각들이다. 기록 목록을 받지 못했으면 비어 있다.
    val completedCheckTimes: Set<LocalTime> = emptySet(),
)

private val checkTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 서버가 내려준 "HH:mm" 체크 시각 목록으로 [CheckSchedule]을 만든다.
 * 형식이 잘못된 항목은 건너뛰고, 중복을 제거해 시간순으로 정렬한다. [now]와 같은 시각은 "다음 체크"로 본다. */
fun buildCheckSchedule(
    rawCheckTimes: List<String>,
    now: LocalTime,
    todayRecordTimes: List<LocalTime> = emptyList(),
): CheckSchedule {
    val checkTimes = rawCheckTimes
        .mapNotNull { rawCheckTime -> rawCheckTime.toCheckTimeOrNull() }
        .distinct()
        .sorted()
    val nextCheckTime = checkTimes.firstOrNull { checkTime -> !checkTime.isBefore(now.withSecond(0).withNano(0)) }
    val minutesUntilNextCheck = nextCheckTime?.let { checkTime ->
        Duration.between(now.withSecond(0).withNano(0), checkTime).toMinutes()
    }

    return CheckSchedule(
        checkTimes = checkTimes,
        nextCheckTime = nextCheckTime,
        minutesUntilNextCheck = minutesUntilNextCheck,
        completedCheckTimes = findCompletedCheckTimes(
            checkTimes = checkTimes,
            todayRecordTimes = todayRecordTimes,
        ),
    )
}

/** 체크 시각마다 "그 시각부터 다음 체크 시각 전까지" 저장된 기록이 하나라도 있으면 완료로 본다(마지막 체크는 자정 전까지).
 * 서버에 체크 완료 여부 필드가 없어 앱이 오늘 기록의 측정 시각으로 판정하는 규칙이며, 백엔드 확인이 필요하다.
 * 첫 체크 시각 이전 기록은 어느 체크에도 넣지 않는다. */
fun findCompletedCheckTimes(
    checkTimes: List<LocalTime>,
    todayRecordTimes: List<LocalTime>,
): Set<LocalTime> {
    return checkTimes
        .filterIndexed { checkIndex, checkTime ->
            val nextCheckTime = checkTimes.getOrNull(checkIndex + 1)
            todayRecordTimes.any { recordTime ->
                !recordTime.isBefore(checkTime) &&
                    (nextCheckTime == null || recordTime.isBefore(nextCheckTime))
            }
        }
        .toSet()
}

private fun String.toCheckTimeOrNull(): LocalTime? {
    return try {
        LocalTime.parse(trim(), checkTimeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}
