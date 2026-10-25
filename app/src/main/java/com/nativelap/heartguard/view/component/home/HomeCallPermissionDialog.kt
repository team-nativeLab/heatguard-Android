package com.nativelap.heartguard.view.component.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R

@Composable
fun HomeCallPermissionDialog(
    onDismissRequest: () -> Unit,
    onOpenSettingsClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.home_call_permission_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.home_call_permission_dialog_message))
        },
        confirmButton = {
            TextButton(onClick = onOpenSettingsClick) {
                Text(text = stringResource(R.string.home_call_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.home_call_permission_dialog_dismiss))
            }
        },
    )
}
