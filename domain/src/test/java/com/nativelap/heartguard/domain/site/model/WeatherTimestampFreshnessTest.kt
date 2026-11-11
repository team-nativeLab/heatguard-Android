package com.nativelap.heartguard.domain.site.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WeatherTimestampFreshnessTest {
    private val now = Instant.parse("2026-11-11T04:00:00Z")

    @Test
    fun timestampJustUnderSixtyMinutesIsFresh() {
        val observedAt = now.minusSeconds(3_599).toString()

        assertTrue(observedAt.isFreshWeatherTimestamp(now))
    }

    @Test
    fun timestampAtSixtyMinutesIsExpired() {
        val observedAt = now.minusSeconds(3_600).toString()

        assertFalse(observedAt.isFreshWeatherTimestamp(now))
    }

    @Test
    fun missingMalformedAndFutureTimestampsAreNotFresh() {
        assertFalse(null.isFreshWeatherTimestamp(now))
        assertFalse("2026-11-11T03:00:00".isFreshWeatherTimestamp(now))
        assertFalse("invalid".isFreshWeatherTimestamp(now))
        assertFalse(now.plusSeconds(1).toString().isFreshWeatherTimestamp(now))
    }

    @Test
    fun elapsedMinutesRequireValidNonFutureOffsetTimestamp() {
        assertEquals(8L, "2026-11-11T03:52:00Z".elapsedWeatherMinutes(now))
        assertNull("2026-11-11T03:52:00".elapsedWeatherMinutes(now))
        assertNull(now.plusSeconds(60).toString().elapsedWeatherMinutes(now))
    }
}
