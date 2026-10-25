package com.nativelap.heartguard.data.profile.mapper

import com.nativelap.heartguard.data.profile.dto.WorkerProfileResponseDto
import com.nativelap.heartguard.domain.profile.model.WorkerProfile

// 빈 문자열은 "값 없음"으로 정규화해 화면이 "--"로 표시하게 한다.
internal fun WorkerProfileResponseDto.toDomain(): WorkerProfile =
    WorkerProfile(
        userId = userId,
        name = name?.takeIf { workerName -> workerName.isNotBlank() },
        email = email?.takeIf { workerEmail -> workerEmail.isNotBlank() },
        companyName = companyName?.takeIf { workerCompanyName -> workerCompanyName.isNotBlank() },
        version = version,
    )
