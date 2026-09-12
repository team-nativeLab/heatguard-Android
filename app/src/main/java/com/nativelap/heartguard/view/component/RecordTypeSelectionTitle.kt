package com.nativelap.heartguard.view.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.R

/** 기록 유형 선택 영역의 중앙 제목을 표시한다. */
@Composable
fun RecordTypeSelectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = HeartGuardFontSize.PageTitle,
            lineHeight = HeartGuardFontSize.PageTitle,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordTypeSelectionTitlePreview() {
    HeartGuardTheme {
        RecordTypeSelectionTitle(title = stringResource(R.string.record_type_selection_title))
    }
}
