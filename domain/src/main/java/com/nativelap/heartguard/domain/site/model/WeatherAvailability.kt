package com.nativelap.heartguard.domain.site.model

/** 현재 위치 날씨 조회값의 서버 가용 상태다. */
enum class WeatherAvailability {
    AVAILABLE,
    CACHED,
    UNAVAILABLE,
    UNKNOWN,
}
