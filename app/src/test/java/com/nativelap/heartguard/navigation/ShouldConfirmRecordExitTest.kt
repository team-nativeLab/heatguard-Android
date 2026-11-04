package com.nativelap.heartguard.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldConfirmRecordExitTest {
    @Test
    fun `temperature record opened from home asks before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.TemperatureRecord,
            )

        assertTrue(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `field photo opened from home asks before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.FieldPhoto,
            )

        assertTrue(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `rest photo opened from home asks before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.RestPhoto,
            )

        assertTrue(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `field photo opened inside the record flow does not ask before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.TemperatureRecord,
                HeartGuardDestination.FieldPhoto,
            )

        assertFalse(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `work photo opened from record history asks before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.RecordHistory,
                HeartGuardDestination.WorkPhoto,
            )

        assertTrue(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `checklist opened from home does not ask before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
                HeartGuardDestination.Checklist,
            )

        assertFalse(shouldConfirmRecordExit(backStack))
    }

    @Test
    fun `home alone does not ask before leaving`() {
        val backStack =
            listOf(
                HeartGuardDestination.Home,
            )

        assertFalse(shouldConfirmRecordExit(backStack))
    }
}
