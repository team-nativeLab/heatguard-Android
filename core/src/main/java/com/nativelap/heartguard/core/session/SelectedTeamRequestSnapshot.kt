package com.nativelap.heartguard.core.session

/** 체크리스트 Call 생성 시 고정해 전송 시점에 선택 변경을 감지한다. */
data class SelectedTeamRequestSnapshot(
    val teamId: String,
    val selectionVersion: Long,
)
