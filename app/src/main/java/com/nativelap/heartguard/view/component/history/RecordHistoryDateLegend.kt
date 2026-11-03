package com.nativelap.heartguard.view.component.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.extraColors

// 문구 앞의 점은 별도 View가 아니라 글리프로 그린다. 두 줄로 바뀌어도 점이 첫 줄 앞에 붙도록 한 Text에 둔다.
@Composable
internal fun RecordHistoryDateLegend() {
    val legendText = stringResource(R.string.history_date_picker_legend)
    val dotColor = MaterialTheme.colorScheme.primary
    val legendAnnotatedText =
        buildAnnotatedString {
            withStyle(SpanStyle(color = dotColor)) {
                append(LEGEND_DOT_GLYPH)
            }
            append(" ")
            append(legendText)
        }

    Text(
        text = legendAnnotatedText,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.extraColors.captionText,
        textAlign = TextAlign.Center,
        style =
            MaterialTheme.typography.bodySmall.copy(
                fontSize = HeartGuardFontSize.RecordDateLegend,
                fontWeight = FontWeight.Normal,
            ),
    )
}

private const val LEGEND_DOT_GLYPH = "•"
