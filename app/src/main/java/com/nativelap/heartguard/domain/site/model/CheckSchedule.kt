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
)

private val checkTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** 서버가 내려준 "HH:mm" 체크 시각 목록으로 [CheckSchedule]을 만든다.
 * 형식이 잘못된 항목은 건너뛰고, 중복을 제거해 시간순으로 정렬한다. [now]와 같은 시각은 "다음 체크"로 본다. */
fun buildCheckSchedule(
    rawCheckTimes: List<String>,
    now: LocalTime,
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
    )
}

private fun String.toCheckTimeOrNull(): LocalTime? {
    return try {
        LocalTime.parse(trim(), checkTimeFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}
