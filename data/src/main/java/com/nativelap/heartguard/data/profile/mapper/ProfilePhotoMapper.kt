package com.nativelap.heartguard.data.profile.mapper

import com.nativelap.heartguard.data.profile.dto.ProfilePhotoResponseDto
import com.nativelap.heartguard.domain.profile.model.ProfilePhoto

fun ProfilePhotoResponseDto.toDomain(): ProfilePhoto =
    ProfilePhoto(
        photoUrl = photoUrl,
        expiresAt = expiresAt,
    )
