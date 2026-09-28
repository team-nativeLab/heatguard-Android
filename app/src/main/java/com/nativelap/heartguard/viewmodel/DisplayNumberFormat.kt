package com.nativelap.heartguard.viewmodel

/** 서버 수치를 단위 없는 표시용 문자열로 바꾼다. 55.0처럼 소수부가 0이면 "55"로, 55.5는 그대로 "55.5"로 보여준다. */
fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}

/** 기상 관측값이 없는 경우 홈과 기록 화면에서 값 없음 상태를 표시한다. */
fun Double?.toDisplayNumber(): String? = this?.toDisplayNumber()
