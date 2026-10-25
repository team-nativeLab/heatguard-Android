package com.nativelap.heartguard.view.component.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeEmergencyPhoneCallDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dialogShowsOnlyPhoneNumberAndCancelDoesNotRequestCall() {
        var didDismiss = false
        var didRequestCall = false
        composeTestRule.setContent {
            HeartGuardTheme {
                HomeEmergencyPhoneCallDialog(
                    phoneNumber = "010-5080-4623",
                    onDismissRequest = { didDismiss = true },
                    onCallClick = { didRequestCall = true },
                )
            }
        }

        composeTestRule.onNodeWithText("010-5080-4623").assertExists()
        composeTestRule.onNodeWithText("취소").performClick()

        composeTestRule.runOnIdle {
            assertEquals(true, didDismiss)
            assertEquals(false, didRequestCall)
        }
    }

    @Test
    fun callButtonInvokesCallCallback() {
        var didRequestCall = false
        composeTestRule.setContent {
            HeartGuardTheme {
                HomeEmergencyPhoneCallDialog(
                    phoneNumber = "010-5080-4623",
                    onDismissRequest = {},
                    onCallClick = { didRequestCall = true },
                )
            }
        }

        composeTestRule.onNodeWithText("통화").performClick()

        composeTestRule.runOnIdle {
            assertEquals(true, didRequestCall)
        }
    }
}
