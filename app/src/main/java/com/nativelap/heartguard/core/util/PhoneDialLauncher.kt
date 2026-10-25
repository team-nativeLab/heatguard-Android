package com.nativelap.heartguard.core.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 화면에 표시되는 전화번호 문자열(공백·하이픈 포함)에서 다이얼러가 이해하는 숫자만 남긴다. */
private fun String.toDialablePhoneNumber(): String = filter { character -> character.isDigit() || character == '+' }

/** 전화번호로 다이얼러를 열 수 있는 콜백을 만든다. */
@Composable
fun rememberPhoneDialLauncher(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { phoneNumber: String ->
            val dialIntent =
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.fromParts("tel", phoneNumber.toDialablePhoneNumber(), null),
                )
            try {
                context.startActivity(dialIntent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }
    }
}
