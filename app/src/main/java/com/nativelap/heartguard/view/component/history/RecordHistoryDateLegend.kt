package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors

@Composable
internal fun RecordHistoryDateLegend() {
    Row(
        modifier = Modifier.padding(horizontal = HeartGuardSpacing.Tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HeartGuardComponentSize.RecordDateDot)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(modifier = Modifier.width(HeartGuardSpacing.Compact))

        Text(
            text = stringResource(R.string.history_date_picker_legend),
            color = MaterialTheme.extraColors.captionText,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = HeartGuardFontSize.RecordDateLegend,
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}
