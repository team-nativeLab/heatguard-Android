package com.nativelap.heartguard.view.screen.account

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.viewmodel.account.WithdrawSubmissionState
import com.nativelap.heartguard.viewmodel.account.WithdrawUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WithdrawResponsiveTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun wideNoticeKeepsActionCenteredWithin600DpContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                HeartGuardTheme {
                    Box(Modifier.requiredSize(840.dp, 582.dp)) {
                        WithdrawNoticeScreen(WithdrawUiState(), "", {})
                    }
                }
            }
        }
        val actionBounds = composeTestRule.onNodeWithText("탈퇴하기").fetchSemanticsNode().boundsInRoot
        assertTrue(actionBounds.width <= 544f)
        assertEquals(420f, actionBounds.center.x, 1f)
        val agreement = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.withdraw_agree)
        composeTestRule.onNodeWithContentDescription(agreement).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun shortExpandedDonePageAllowsScrollingToMessageAndConfirmation() {
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(originalDensity.density, 2f)) {
                HeartGuardTheme {
                    Box(Modifier.requiredSize(393.dp, 420.dp)) {
                        WithdrawDoneScreen({})
                    }
                }
            }
        }
        val message = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.withdraw_done_message)
        composeTestRule.onNodeWithText(message).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("확인").assertIsDisplayed()
    }

    @Test
    fun expandedNoticeWithKeyboardAndFailureKeepsPasswordAndAgreementReachable() {
        var isKeyboardVisible = false
        composeTestRule.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(composeTestRule.activity.window, false)
            composeTestRule.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(originalDensity.density, 2f)) {
                val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
                SideEffect { isKeyboardVisible = keyboardHeight > 0 }
                HeartGuardTheme {
                    Box(Modifier.requiredSize(393.dp, 582.dp)) {
                        WithdrawNoticeScreen(
                            uiState = WithdrawUiState(submissionState = WithdrawSubmissionState.Failed),
                            password = "",
                            onEvent = {},
                        )
                    }
                }
            }
        }
        val password = composeTestRule.onAllNodes(hasSetTextAction())[0]
        password.performScrollTo().performTouchInput { click() }
        composeTestRule.waitUntil(5_000) { isKeyboardVisible }
        password.performScrollTo().assertIsDisplayed()
        val passwordNode = password.fetchSemanticsNode()
        assertTrue(passwordNode.boundsInRoot.height >= passwordNode.size.height - 1f)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.withdraw_agree))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.withdraw_failure_message))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("탈퇴하기").performScrollTo().assertIsDisplayed()
    }
}
