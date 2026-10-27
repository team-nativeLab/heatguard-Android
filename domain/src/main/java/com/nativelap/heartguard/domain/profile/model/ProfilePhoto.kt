package com.nativelap.heartguard.domain.profile.model

/** 작업자 프로필 사진의 짧은 수명 조회 URL이다. */
data class ProfilePhoto(
    val photoUrl: String?,
    val expiresAt: String?,
)
