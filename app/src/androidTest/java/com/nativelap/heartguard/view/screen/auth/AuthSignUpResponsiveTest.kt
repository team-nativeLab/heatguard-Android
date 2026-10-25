package com.nativelap.heartguard.view.screen.auth

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.espresso.Espresso
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
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
        composeTestRule
            .onAllNodes(hasText("회원가입") and hasClickAction())[0]
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").assertIsDisplayed()
        val actualRightPadding = emailFieldBounds.right - buttonBounds.right
        assertTrue(
            "Expected right padding $expectedRightPadding px but was $actualRightPadding px",
            abs(actualRightPadding - expectedRightPadding) < 2f,
        )
        assertTrue(composeTestRule.onAllNodes(hasText("앱 계정 로그인")).fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithText("로그인").assertIsDisplayed()

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

    @Test
    fun signupInputFieldsUseTheSameHeight() {
        composeTestRule.setContent {
            HeartGuardTheme {
                Box(Modifier.requiredSize(393.dp, 874.dp)) {
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
                        onEvent = {},
                    )
                }
            }
        }

        val inputFieldHeights =
            composeTestRule
                .onAllNodes(hasSetTextAction())
                .fetchSemanticsNodes()
                .map { semanticsNode -> semanticsNode.boundsInRoot.height }

        assertEquals(4, inputFieldHeights.size)
        inputFieldHeights.forEach { inputFieldHeight ->
            assertTrue(
                "Expected all sign-up inputs to have equal height: $inputFieldHeights",
                abs(inputFieldHeight - inputFieldHeights.first()) < 1f,
            )
        }
    }

    @Test
    fun emailAndVerificationCodeFieldsUseTheCloseFigmaSpacing() {
        composeTestRule.setContent {
            HeartGuardTheme {
                Box(Modifier.requiredSize(393.dp, 874.dp)) {
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
                        onEvent = {},
                    )
                }
            }
        }

        val emailFieldBounds =
            composeTestRule
                .onAllNodes(hasSetTextAction())[0]
                .fetchSemanticsNode()
                .boundsInRoot
        val verificationLabelBounds =
            composeTestRule
                .onNodeWithText("이메일 인증코드")
                .fetchSemanticsNode()
                .boundsInRoot
        val expectedGap = with(composeTestRule.density) { HeartGuardSpacing.AuthSignUpEmailCodeGap.toPx() }
        val actualGap = verificationLabelBounds.top - emailFieldBounds.bottom

        assertTrue(
            "Expected email-to-code gap $expectedGap px but was $actualGap px",
            abs(actualGap - expectedGap) < 1f,
        )
    }

    @Test
    fun shortViewportPinsActionsAndKeepsLastFormFieldScrollable() {
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
                        onEvent = {},
                    )
                }
            }
        }

        val signUpButton = composeTestRule.onAllNodes(hasText("회원가입") and hasClickAction())[0]
        val loginFooter = composeTestRule.onNodeWithText("로그인")
        signUpButton.assertIsDisplayed()
        loginFooter.assertIsDisplayed()

        val loginFooterBounds = loginFooter.fetchSemanticsNode().boundsInRoot
        val viewportHeight = with(composeTestRule.density) { 582.dp.toPx() }
        val bottomGap = viewportHeight - loginFooterBounds.bottom
        val maximumBottomGap = with(composeTestRule.density) { 100.dp.toPx() }
        assertTrue(
            "Expected login footer near viewport bottom, gap was $bottomGap px",
            bottomGap in 0f..maximumBottomGap,
        )

        composeTestRule
            .onNodeWithText("비밀번호를 한 번 더 입력해주세요")
            .performScrollTo()
            .assertIsDisplayed()
        loginFooter.assertIsDisplayed()
    }

    @Test
    fun focusingLastFieldShowsImePreservesInputAndRestoresFooterAfterClosing() {
        val confirmationState = mutableStateOf("")
        var isKeyboardVisible = false
        composeTestRule.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(composeTestRule.activity.window, false)
            composeTestRule.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        composeTestRule.setContent {
            val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
            SideEffect {
                isKeyboardVisible = keyboardHeight > 0
            }
            HeartGuardTheme {
                Box(Modifier.requiredSize(393.dp, 582.dp)) {
                    AuthSignUpScreen(
                        email = "",
                        verificationCode = "",
                        password = "",
                        passwordConfirmation = confirmationState.value,
                        isSendingEmailCode = false,
                        isEmailCodeSent = false,
                        resendCooldownSeconds = 0,
                        isSubmitting = false,
                        statusMessage = null,
                        onEvent = { event ->
                            if (event is AuthSignUpScreenEvent.PasswordConfirmationChanged) {
                                confirmationState.value = event.passwordConfirmation
                            }
                        },
                    )
                }
            }
        }

        val confirmationField = composeTestRule.onAllNodes(hasSetTextAction())[3]
        confirmationField.performScrollTo().performTouchInput { click() }
        composeTestRule.waitUntil(5_000) {
            val fieldNode = confirmationField.fetchSemanticsNode()
            isKeyboardVisible && fieldNode.boundsInRoot.height >= fieldNode.size.height - 1f
        }
        confirmationField
            .assertIsFocused()
            .assertIsDisplayed()
            .performTextInput("confirm-check")
        composeTestRule.waitUntil(5_000) { confirmationState.value == "confirm-check" }
        composeTestRule.runOnIdle { assertEquals("confirm-check", confirmationState.value) }
        composeTestRule
            .onAllNodes(hasText("회원가입") and hasClickAction())[0]
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").performScrollTo().assertIsDisplayed()

        Espresso.pressBack()
        composeTestRule.waitUntil(5_000) { !isKeyboardVisible }
        composeTestRule
            .onAllNodes(hasText("회원가입") and hasClickAction())[0]
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").assertIsDisplayed()
    }
}
