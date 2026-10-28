package com.nativelap.heartguard.core.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal fun String.toDialablePhoneNumberOrNull(): String? {
    val candidate = trim()
    if (candidate.isEmpty()) {
        return null
    }

    if (candidate.any { character ->
            !character.isAsciiPhoneDigit() &&
                !character.isWhitespace() &&
                character != '-' &&
                character != '(' &&
                character != ')' &&
                character != '+'
        }
    ) {
        return null
    }

    if (candidate.drop(1).contains('+')) {
        return null
    }

    var isInsideParentheses = false
    var hasDigitInsideParentheses = false
    var digitCount = 0

    candidate.forEach { character ->
        when {
            character.isAsciiPhoneDigit() -> {
                digitCount += 1
                if (isInsideParentheses) {
                    hasDigitInsideParentheses = true
                }
            }

            character == '(' -> {
                if (isInsideParentheses) {
                    return null
                }
                isInsideParentheses = true
                hasDigitInsideParentheses = false
            }

            character == ')' -> {
                if (!isInsideParentheses || !hasDigitInsideParentheses) {
                    return null
                }
                isInsideParentheses = false
            }
        }
    }

    if (isInsideParentheses || digitCount !in MIN_PHONE_NUMBER_DIGITS..MAX_PHONE_NUMBER_DIGITS) {
        return null
    }

    return candidate.filter { character -> character.isAsciiPhoneDigit() || character == '+' }
}

internal fun String.isValidPhoneNumber(): Boolean = toDialablePhoneNumberOrNull() != null

private fun Char.isAsciiPhoneDigit(): Boolean = this in '0'..'9'

/** 전화번호로 다이얼러를 열 수 있는 콜백을 만든다. */
@Composable
fun rememberPhoneDialLauncher(): (String) -> Boolean = rememberPhoneIntentLauncher(Intent.ACTION_DIAL)

@Composable
private fun rememberPhoneIntentLauncher(action: String): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context, action) {
        { phoneNumber: String ->
            val dialablePhoneNumber = phoneNumber.toDialablePhoneNumberOrNull()
            if (dialablePhoneNumber == null) {
                false
            } else {
                val phoneIntent =
                    Intent(
                        action,
                        Uri.fromParts("tel", dialablePhoneNumber, null),
                    )
                try {
                    context.startActivity(phoneIntent)
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                } catch (_: SecurityException) {
                    false
                }
            }
        }
    }
}

private const val MIN_PHONE_NUMBER_DIGITS = 7
private const val MAX_PHONE_NUMBER_DIGITS = 15
