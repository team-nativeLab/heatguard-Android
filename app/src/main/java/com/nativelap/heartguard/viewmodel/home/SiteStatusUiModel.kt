package com.nativelap.heartguard.viewmodel.home

import androidx.compose.runtime.Immutable
import com.nativelap.heartguard.domain.site.model.TeamSiteOverview
import com.nativelap.heartguard.viewmodel.toDisplayNumber

/** 팀 현장페이지 응답 중 여러 화면(홈·온도계 기록·현장 사진·긴급 호출)이 함께 보여주는 값이다.
 * 서버 응답을 아직 받지 못했거나 실패했으면 각 값이 null이며, 화면은 이를 "--"로 표시한다.
 * 숫자는 단위 없이 표시용 문자열로만 바꿔 두고, 단위·문구 조합은 화면의 문자열 리소스가 맡는다. */
@Immutable
data class SiteStatusUiModel(
    val temperature: String? = null,
    val humidity: String? = null,
    val apparentTemperature: String? = null,
    val heatLevel: Int? = null,
    val managerPhoneNumber: String? = null,
)

/** 홈 화면 상태를 공용 현장 상태로 바꾼다. Success가 아니면 모든 값이 null이다. */
fun HomeUiState.toSiteStatusUiModel(): SiteStatusUiModel {
    val overview = (this as? HomeUiState.Success)?.overview ?: return SiteStatusUiModel()

    return overview.toSiteStatusUiModel()
}

private fun TeamSiteOverview.toSiteStatusUiModel(): SiteStatusUiModel {
    return SiteStatusUiModel(
        temperature = currentTemperature.toDisplayNumber(),
        humidity = humidity.toDisplayNumber(),
        apparentTemperature = apparentTemperature.toDisplayNumber(),
        heatLevel = heatLevel,
        managerPhoneNumber = managerPhoneNumber.ifBlank { null },
    )
}
