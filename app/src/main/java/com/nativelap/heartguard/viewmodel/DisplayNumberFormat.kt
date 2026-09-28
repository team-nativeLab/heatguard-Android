package com.nativelap.heartguard.viewmodel

/** 서버 수치를 단위 없는 표시용 문자열로 바꾼다. 55.0처럼 소수부가 0이면 "55"로, 55.5는 그대로 "55.5"로 보여준다. */
fun Double.toDisplayNumber(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}
