package com.nativelap.heartguard.domain.site.model

/** 현장관리자가 수동으로 입력한 하늘 상태다. 서버가 새 값을 보내면 UNKNOWN으로 받는다. */
enum class SkyStatus {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    UNKNOWN,
}
