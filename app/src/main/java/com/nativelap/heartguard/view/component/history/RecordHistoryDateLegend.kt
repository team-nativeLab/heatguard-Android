package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.extraColors

// 범례 점과 문구 사이 간격이다. Figma 범례 기준 6dp이며 이 범례에서만 쓴다.
private val dotToLegendTextGap = 6.dp

@Composable
internal fun RecordHistoryDateLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardComponentSize.RecordDateDot)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(modifier = Modifier.width(dotToLegendTextGap))

        Text(
            text = stringResource(R.string.history_date_picker_legend),
            color = MaterialTheme.extraColors.captionText,
            textAlign = TextAlign.Center,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = HeartGuardFontSize.RecordDateLegend,
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}
