package com.nativelap.heartguard.view.route.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nativelap.heartguard.core.component.overlay.ApplyDialogWindowBackgroundBlur
import com.nativelap.heartguard.ui.theme.HeartGuardOverlayBlur
import com.nativelap.heartguard.view.component.home.HomeCallPermissionDialog

@Composable
internal fun HomeHeadquartersCallPermissionRoute(onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    ApplyDialogWindowBackgroundBlur(
        blurRadius = HeartGuardOverlayBlur.FigmaBackdrop,
    )

    HomeCallPermissionDialog(
        onDismissRequest = onDismissRequest,
        onOpenSettingsClick = {
            onDismissRequest()
            val settingsIntent =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            context.startActivity(settingsIntent)
        },
    )
}
