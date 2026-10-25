package com.nativelap.heartguard.view.component.password

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

/** 입력칸 아래 안내 문구다. 기본은 회색, 조건 충족은 파랑(✓), 오류는 빨강이다(Figma 26~28).
 * 상태가 바뀔 때 스크린리더가 읽도록 liveRegion을 둔다. */
@Composable
fun PasswordGuideText(
    guideText: String,
    guideState: PasswordGuideState,
    modifier: Modifier = Modifier,
) {
    Text(
        text = guideText,
        modifier =
            modifier
                .padding(top = HeartGuardSpacing.Tight)
                .semantics { liveRegion = LiveRegionMode.Polite },
        color =
            when (guideState) {
                PasswordGuideState.NEUTRAL -> MaterialTheme.extraColors.secondaryText
                PasswordGuideState.SATISFIED -> MaterialTheme.colorScheme.primary
                PasswordGuideState.ERROR -> MaterialTheme.colorScheme.error
            },
        style = MaterialTheme.typography.bodySmall,
    )
}

enum class PasswordGuideState {
    NEUTRAL,
    SATISFIED,
    ERROR,
}

@Preview(showBackground = true)
@Composable
private fun PasswordGuideTextPreview() {
    HeartGuardTheme {
        PasswordGuideText(
            guideText = "✓ 영문, 숫자를 포함해 8자 이상",
            guideState = PasswordGuideState.SATISFIED,
        )
    }
}
