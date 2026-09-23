package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors

@Immutable
data class SavedRecordSummaryItem(
    val label: String,
    val value: String,
    val hasDetails: Boolean = false,
    val detail: String? = null,
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
        modifier = modifier
            .fillMaxWidth()
            .height(HeartGuardComponentSize.ResultSummaryHeight),
        shape = RoundedCornerShape(HeartGuardRadius.LargeCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = HeartGuardSpacing.Hairline,
            color = MaterialTheme.extraColors.cardBorder,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = HeartGuardSpacing.ResultSummaryHorizontal,
                    end = HeartGuardSpacing.ResultErrorDetailHorizontal,
                    top = HeartGuardSpacing.Item,
                    bottom = HeartGuardSpacing.Section,
                ),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = HeartGuardFontSize.ResultSummaryTitle,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(modifier = Modifier.height(HeartGuardSpacing.Compact))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldInset),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            records.forEachIndexed { index, record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeartGuardSpacing.ResultSummaryRowHeight)
                        .padding(start = HeartGuardSpacing.ResultSummaryRowIndent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = HeartGuardFontSize.ResultSummaryLabel,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Row(
                            modifier = Modifier.padding(start = HeartGuardSpacing.RecordFieldInset),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = record.value,
                                color = MaterialTheme.extraColors.mutedText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = HeartGuardFontSize.ResultSummaryValue,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            record.detail?.let { detailText ->
                                Text(
                                    text = detailText,
                                    color = MaterialTheme.extraColors.mutedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = HeartGuardFontSize.ResultSummaryValue,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }
                    if (record.hasDetails) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < records.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = HeartGuardSpacing.RecordFieldInset),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
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
