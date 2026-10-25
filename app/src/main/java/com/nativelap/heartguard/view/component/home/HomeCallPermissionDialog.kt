package com.nativelap.heartguard.view.component.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardComponentSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

@Composable
fun HomeCallPermissionDialog(
    onDismissRequest: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .widthIn(max = HeartGuardComponentSize.DialogMaxWidth)
                .fillMaxWidth(),
        shape = RoundedCornerShape(HeartGuardRadius.Dialog),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.padding(HeartGuardSpacing.Section),
            verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
        ) {
            Text(
                text = stringResource(R.string.home_call_permission_dialog_title),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_call_permission_dialog_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        HeartGuardSpacing.Compact,
                        Alignment.End,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.home_call_permission_dialog_dismiss))
                }
                TextButton(onClick = onOpenSettingsClick) {
                    Text(text = stringResource(R.string.home_call_open_settings))
                }
            }
        }
    }
}
