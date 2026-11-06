package com.nativelap.heartguard.viewmodel.record

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordDraftUiStateTest {
    @Test
    fun allowsManualTemperatureWithoutSiteBaseline() {
        val draftState =
            RecordDraftUiState(
                temperatureText = "31.5",
                humidityText = "55",
                isManualInputEnabled = true,
            )

        assertTrue(draftState.canSubmitTemperatureRecord(hasSiteWeatherBaseline = false))
    }

    @Test
    fun rejectsIncompleteManualTemperatureEvenWhenSiteBaselineExists() {
        val draftState =
            RecordDraftUiState(
                temperatureText = "31.5",
                humidityText = "",
                isManualInputEnabled = true,
            )

        assertFalse(draftState.canSubmitTemperatureRecord(hasSiteWeatherBaseline = true))
    }

    @Test
    fun allowsSiteBaselineWhenManualInputIsDisabled() {
        val draftState = RecordDraftUiState()

        assertTrue(draftState.canSubmitTemperatureRecord(hasSiteWeatherBaseline = true))
    }

    @Test
    fun rejectsMissingSiteBaselineWhenManualInputIsDisabled() {
        val draftState = RecordDraftUiState()

        assertFalse(draftState.canSubmitTemperatureRecord(hasSiteWeatherBaseline = false))
    }
}
