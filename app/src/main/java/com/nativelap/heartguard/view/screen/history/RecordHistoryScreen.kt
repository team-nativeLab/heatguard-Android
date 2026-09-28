package com.nativelap.heartguard.view.screen.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.heartGuardResponsivePage
import kotlinx.coroutines.launch

@Composable
fun RecordHistoryScreen(
    onBackClick: () -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unavailableMessage = stringResource(R.string.history_unavailable_message)
    val showUnavailableMessage: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message = unavailableMessage)
        }
        Unit
    }

    Scaffold(
        modifier = modifier.heartGuardResponsivePage(MaterialTheme.extraColors.pageBackground),
        containerColor = MaterialTheme.extraColors.pageBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = HeartGuardSpacing.Compact),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.extraColors.pageBackground,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "‹",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.home_record_history),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        },
        bottomBar = {
            Button(
                onClick = onRecordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HeartGuardSpacing.Section,
                        vertical = HeartGuardSpacing.Compact,
                    )
                    .height(48.dp),
                shape = RoundedCornerShape(HeartGuardRadius.Button),
            ) {
                Text(text = stringResource(R.string.history_record_action))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = HeartGuardSpacing.Section),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HeartGuardSpacing.Section),
                verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
            ) {
                Surface(
                    onClick = showUnavailableMessage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.extraColors.homeTimelineTrack),
                    enabled = true,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = HeartGuardSpacing.Compact),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "▦", color = MaterialTheme.extraColors.homeMutedText)
                        Spacer(modifier = Modifier.width(HeartGuardSpacing.Compact))
                        Text(
                            text = stringResource(R.string.history_period_unavailable),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.extraColors.disabledContent,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(text = "⌄", color = MaterialTheme.extraColors.disabledContent)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryFilterChip(stringResource(R.string.history_filter_all), onClick = showUnavailableMessage)
                    HistoryFilterChip(stringResource(R.string.history_filter_temperature), onClick = showUnavailableMessage)
                    HistoryFilterChip(stringResource(R.string.history_filter_work_photo), onClick = showUnavailableMessage)
                    HistoryFilterChip(stringResource(R.string.history_filter_rest_photo), onClick = showUnavailableMessage)
                }
                HistorySummaryCard()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.extraColors.homeMetricContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.home_history),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.history_unavailable_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.history_unavailable_description),
                        color = MaterialTheme.extraColors.homeMutedText,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.extraColors.homeTimelineTrack),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = MaterialTheme.extraColors.disabledContent,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun HistorySummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HeartGuardSpacing.Compact, vertical = HeartGuardSpacing.Section),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistorySummaryItem(stringResource(R.string.history_filter_all))
            HistorySummaryItem(stringResource(R.string.history_filter_temperature))
            HistorySummaryItem(stringResource(R.string.history_filter_work_photo))
            HistorySummaryItem(stringResource(R.string.history_filter_rest_photo))
        }
    }
}

@Composable
private fun RowScope.HistorySummaryItem(label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.extraColors.homeMutedText,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "—",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun RecordHistoryScreenPreview() {
    HeartGuardTheme {
        RecordHistoryScreen(onBackClick = {}, onRecordClick = {})
    }
}

@Preview(name = "Compact large text", showBackground = true, widthDp = 320, heightDp = 740, fontScale = 2f)
@Composable
private fun RecordHistoryScreenCompactPreview() {
    HeartGuardTheme {
        RecordHistoryScreen(onBackClick = {}, onRecordClick = {})
    }
}

@Preview(name = "Wide", showBackground = true, widthDp = 840, heightDp = 1024)
@Composable
private fun RecordHistoryScreenWidePreview() {
    HeartGuardTheme {
        RecordHistoryScreen(onBackClick = {}, onRecordClick = {})
    }
}
