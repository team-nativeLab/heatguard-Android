package com.nativelap.heartguard.view.screen.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class AuthSignUpResponsiveTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emailCodeActionStaysInsideEmailFieldAndRequestsCode() {
        var receivedEvent: AuthSignUpScreenEvent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                Box(Modifier.requiredSize(393.dp, 582.dp)) {
                    AuthSignUpScreen(
                        email = "",
                        verificationCode = "",
                        password = "",
                        passwordConfirmation = "",
                        isSendingEmailCode = false,
                        isEmailCodeSent = false,
                        resendCooldownSeconds = 0,
                        isSubmitting = false,
                        statusMessage = null,
                        onEvent = { event -> receivedEvent = event },
                    )
                }
            }
        }

        val emailFieldBounds =
            composeTestRule
                .onAllNodes(hasSetTextAction())[0]
                .fetchSemanticsNode()
                .boundsInRoot
        val emailCodeButton = composeTestRule.onNodeWithText("인증 받기")
        val buttonBounds = emailCodeButton.fetchSemanticsNode().boundsInRoot
        val expectedRightPadding = with(composeTestRule.density) { 4.dp.toPx() }

        assertTrue(buttonBounds.left >= emailFieldBounds.left)
        assertTrue(buttonBounds.top >= emailFieldBounds.top)
        assertTrue(buttonBounds.bottom <= emailFieldBounds.bottom)
        val actualRightPadding = emailFieldBounds.right - buttonBounds.right
        assertTrue(
            "Expected right padding $expectedRightPadding px but was $actualRightPadding px",
            abs(actualRightPadding - expectedRightPadding) < 2f,
        )
        assertTrue(composeTestRule.onAllNodes(hasText("앱 계정 로그인")).fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithText("로그인").performScrollTo().assertIsDisplayed()

        emailCodeButton.performClick()
        composeTestRule.runOnIdle {
            assertEquals(AuthSignUpScreenEvent.RequestEmailCodeClicked, receivedEvent)
        }
    }

    @Test
    fun shortenedEmailCodeActionRemainsVisibleWithExpandedFontAndCooldown() {
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(originalDensity.density, 2f),
            ) {
                HeartGuardTheme {
                    Box(Modifier.requiredSize(360.dp, 582.dp)) {
                        AuthSignUpScreen(
                            email = "worker@example.com",
                            verificationCode = "",
                            password = "",
                            passwordConfirmation = "",
                            isSendingEmailCode = false,
                            isEmailCodeSent = true,
                            resendCooldownSeconds = 10,
                            isSubmitting = false,
                            statusMessage = null,
                            onEvent = {},
                        )
                    }
                }
            }
        }

        val emailFieldBounds =
            composeTestRule
                .onAllNodes(hasSetTextAction())[0]
                .fetchSemanticsNode()
                .boundsInRoot
        val cooldownLabel = composeTestRule.onNodeWithText("10초")
        cooldownLabel.assertIsDisplayed()
        val buttonBounds = cooldownLabel.fetchSemanticsNode().boundsInRoot

        assertTrue(buttonBounds.left >= emailFieldBounds.left)
        assertTrue(buttonBounds.top >= emailFieldBounds.top)
        assertTrue(buttonBounds.bottom <= emailFieldBounds.bottom)
    }
}
