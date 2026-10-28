package com.nativelap.heartguard.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneNumberValidatorTest {
    @Test
    fun acceptsKoreanPhoneNumberFormats() {
        val koreanFormats =
            listOf(
                "010-1234-5678",
                "(010) 1234-5678",
                "010 1234 5678",
            )

        koreanFormats.forEach { phoneNumber ->
            assertTrue(phoneNumber.isValidPhoneNumber())
        }

        assertEquals("01012345678", "(010) 1234-5678".toDialablePhoneNumberOrNull())
    }

    @Test
    fun acceptsInternationalPhoneNumberFormatsWithLeadingPlus() {
        val internationalFormats =
            listOf(
                "+82 10-1234-5678",
                "+1 (212) 555-0100",
            )

        internationalFormats.forEach { phoneNumber ->
            assertTrue(phoneNumber.isValidPhoneNumber())
        }

        assertEquals("+821012345678", "+82 10-1234-5678".toDialablePhoneNumberOrNull())
    }

    @Test
    fun acceptsSevenToFifteenDigits() {
        assertTrue("1234567".isValidPhoneNumber())
        assertTrue("+123456789012345".isValidPhoneNumber())
    }

    @Test
    fun rejectsTextShortNumbersAndInvalidPlusPlacement() {
        val invalidPhoneNumbers =
            listOf(
                "문의 123",
                "123",
                "123456",
                "12+3456789",
                "+82+10-1234-5678",
                "(010 1234-5678",
                "010-1234-5678-90123",
                "０１０-１２３４-５６７８",
                "٠١٠-١٢٣٤-٥٦٧٨",
            )

        invalidPhoneNumbers.forEach { phoneNumber ->
            assertFalse(phoneNumber.isValidPhoneNumber(), phoneNumber)
            assertNull(phoneNumber.toDialablePhoneNumberOrNull(), phoneNumber)
        }
    }
}
