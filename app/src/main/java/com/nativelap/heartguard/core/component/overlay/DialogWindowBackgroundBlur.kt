package com.nativelap.heartguard.core.component.overlay

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.roundToInt

/** Dialog 또는 ModalBottomSheet Window의 배경을 Figma overlay blur 값으로 관리한다. */
@Composable
fun ApplyDialogWindowBackgroundBlur(
    blurRadius: Dp,
) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    val blurRadiusPx = with(LocalDensity.current) {
        blurRadius.toPx().roundToInt()
    }

    DisposableEffect(dialogWindow, blurRadiusPx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialogWindow?.attributes = dialogWindow.attributes.apply {
                this.blurBehindRadius = blurRadiusPx
            }
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow?.attributes = dialogWindow.attributes.apply {
                    this.blurBehindRadius = 0
                }
            }
        }
    }
}
