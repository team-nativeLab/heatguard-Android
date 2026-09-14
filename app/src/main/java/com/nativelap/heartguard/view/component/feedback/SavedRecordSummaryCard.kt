package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

@Immutable
data class SavedRecordSummaryItem(
    val label: String,
    val value: String,
    val hasDetails: Boolean = false,
)

/** 저장 성공 화면에서 방금 저장한 기록의 주요 값을 행 단위로 보여준다. */
@Composable
fun SavedRecordSummaryCard(
    title: String,
    records: List<SavedRecordSummaryItem>,
    modifier: Modifier = Modifier,
    onDetailsClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            records.forEachIndexed { index, record ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
                ) {
                    Text(
                        text = record.label,
                        modifier = Modifier.weight(0.75f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = record.value,
                        modifier = Modifier.weight(1.25f),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (record.hasDetails) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            onDetailsClick?.let { click ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = click) {
                        Text(text = stringResource(R.string.common_details))
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun SavedRecordSummaryCardPreview() {
    HeartGuardTheme {
        SavedRecordSummaryCard(
            title = "온도 기록",
            records = listOf(
                SavedRecordSummaryItem("현재 온도", "37℃"),
                SavedRecordSummaryItem("체감온도", "40℃"),
                SavedRecordSummaryItem("습도", "65%"),
            ),
        )
    }
}
