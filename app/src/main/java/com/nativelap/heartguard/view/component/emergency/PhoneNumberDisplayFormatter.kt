package com.nativelap.heartguard.view.component.emergency

internal fun formatKoreanPhoneNumberForDisplay(phoneNumber: String): String {
    val digitsOnlyPhoneNumber = phoneNumber.filter { character -> character.isDigit() }

    return if (
        digitsOnlyPhoneNumber.startsWith(KOREAN_MOBILE_PHONE_PREFIX) &&
        digitsOnlyPhoneNumber.length == KOREAN_MOBILE_PHONE_NUMBER_DIGIT_COUNT
    ) {
        "${digitsOnlyPhoneNumber.substring(
            0,
            3,
        )}-${digitsOnlyPhoneNumber.substring(3, 7)}-${digitsOnlyPhoneNumber.substring(7)}"
    } else {
        phoneNumber
    }
}

private const val KOREAN_MOBILE_PHONE_PREFIX = "010"
private const val KOREAN_MOBILE_PHONE_NUMBER_DIGIT_COUNT = 11
