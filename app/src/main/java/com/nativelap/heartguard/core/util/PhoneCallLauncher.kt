package com.nativelap.heartguard.core.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 전화번호로 바로 통화를 시작하고, 실행할 전화 앱이 없으면 false를 반환한다. */
@Composable
fun rememberPhoneCallLauncher(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { phoneNumber: String ->
            val dialablePhoneNumber =
                phoneNumber.filter { character ->
                    character.isDigit() || character == '+'
                }
            val callIntent =
                Intent(
                    Intent.ACTION_CALL,
                    Uri.fromParts("tel", dialablePhoneNumber, null),
                )
            try {
                context.startActivity(callIntent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
