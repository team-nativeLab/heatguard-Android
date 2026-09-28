package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.control.HeartGuardCheckbox
import com.nativelap.heartguard.viewmodel.home.HomeChecklistUiState

@Composable
fun HomeChecklistCard(
    state: HomeChecklistUiState,
    onItemCheckedChange: (itemId: String, checked: Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight),
        ) {
            Text(
                text = stringResource(R.string.home_today_check_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
            when (state) {
                HomeChecklistUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(24.dp),
                    strokeWidth = 2.dp,
                )

                is HomeChecklistUiState.Error -> ChecklistError(onRetry = onRetry)
                is HomeChecklistUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_checklist_empty),
                            color = MaterialTheme.extraColors.homeMutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        state.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.text,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                HeartGuardCheckbox(
                                    isChecked = item.checked,
                                    onCheckedChange = { checked ->
                                        onItemCheckedChange(item.itemId, checked)
                                    },
                                    contentDescription = stringResource(
                                        R.string.home_checklist_item_accessibility,
                                        item.text,
                                    ),
                                    isEnabled = item.itemId !in state.pendingItemIds,
                                )
                            }
                        }
                    }
                    state.updateError?.let {
                        Spacer(modifier = Modifier.height(HeartGuardSpacing.Hairline))
                        ChecklistError(onRetry = onRetry, isUpdateError = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistError(
    onRetry: () -> Unit,
    isUpdateError: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Tight)) {
        Text(
            text = stringResource(
                if (isUpdateError) R.string.home_checklist_update_error else R.string.home_checklist_load_error,
            ),
            color = MaterialTheme.extraColors.homeMutedText,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}
