package com.nativelap.heartguard.core.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 화면에 표시되는 전화번호 문자열(공백·하이픈 포함)에서 다이얼러가 이해하는 숫자만 남긴다. */
private fun String.toDialablePhoneNumber(): String =
    filter { character -> character.isDigit() || character == '+' }

/** 전화번호를 받아 기기 다이얼러 화면을 여는 콜백을 만든다.
 * ACTION_DIAL은 통화 버튼을 누르는 마지막 동작을 사용자가 직접 수행하므로 CALL_PHONE 권한이 필요 없다.
 * 다이얼러 앱 자체가 없는 기기(일부 에뮬레이터 등)에서는 ActivityNotFoundException을 흡수해 앱이
 * 죽지 않도록 하며, Figma/API 명세서에 실패 안내 UI가 정의되어 있지 않아 별도 오류 화면은 만들지 않는다. */
@Composable
fun rememberPhoneDialLauncher(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { phoneNumber: String ->
            val dialIntent = Intent(
                Intent.ACTION_DIAL,
                Uri.fromParts("tel", phoneNumber.toDialablePhoneNumber(), null),
            )
            try {
                context.startActivity(dialIntent)
            } catch (_: ActivityNotFoundException) {
                // 다이얼러 앱이 없는 기기에서는 조용히 무시한다. 기획상 별도 실패 안내가 없다.
            }
        }
    }
}
