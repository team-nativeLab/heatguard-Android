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
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AuthLoginResponsiveTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shortViewportShowsEntirePasswordFieldAboveLoginAction() {
        composeTestRule.setContent {
            HeartGuardTheme {
                Box(Modifier.requiredSize(393.dp, 582.dp)) {
                    AuthLoginScreen("", "", {}, {}, {})
                }
            }
        }
        val password = composeTestRule.onAllNodes(hasSetTextAction())[1]
        password.assertIsDisplayed()
        val passwordNode = password.fetchSemanticsNode()
        val actionBounds = composeTestRule.onNodeWithText("로그인").fetchSemanticsNode().boundsInRoot
        assertTrue(passwordNode.boundsInRoot.bottom < actionBounds.top)
        assertTrue(passwordNode.boundsInRoot.height >= passwordNode.size.height - 1f)
    }

    @Test
    fun expandedFontAndPasswordErrorRemainReachableByScrolling() {
        val errorMessage = "비밀번호를 다시 확인해 주세요. 입력한 내용이 올바른지 확인해 주세요."
        var isKeyboardVisible = false
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(originalDensity.density, 2f)) {
                val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
                SideEffect { isKeyboardVisible = keyboardHeight > 0 }
                HeartGuardTheme {
                    Box(Modifier.requiredSize(393.dp, 582.dp)) {
                        AuthLoginScreen(
                            email = "",
                            password = "",
                            onEmailChange = {},
                            onPasswordChange = {},
                            onLoginClick = {},
                            isPasswordError = true,
                            passwordErrorMessage = errorMessage,
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText(errorMessage).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").assertIsDisplayed()
        composeTestRule.onAllNodes(hasSetTextAction())[1].performClick()
        composeTestRule.waitUntil(5_000) { isKeyboardVisible }
        composeTestRule.onNodeWithText(errorMessage).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun expandedFontWithKeyboardAndSubmissionErrorKeepsFormReachable() {
        val statusMessage = "로그인에 실패했습니다. 잠시 후 다시 시도해주세요"
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
                        AuthLoginScreen("", "", {}, {}, {}, statusMessage = statusMessage)
                    }
                }
            }
        }
        val password = composeTestRule.onAllNodes(hasSetTextAction())[1]
        password.performScrollTo().performTouchInput { click() }
        composeTestRule.waitUntil(5_000) { isKeyboardVisible }
        password.performScrollTo().assertIsDisplayed()
        val passwordNode = password.fetchSemanticsNode()
        assertTrue(passwordNode.boundsInRoot.height >= passwordNode.size.height - 1f)
        composeTestRule.onNodeWithText(statusMessage).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("로그인").performScrollTo().assertIsDisplayed()
    }
    @Test
    fun keyboardTransitionPreservesFocusAndAutomaticallyRevealsPassword() {
        var keyboardVisible = false
        val passwordState = mutableStateOf("")
        composeTestRule.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(composeTestRule.activity.window, false)
            composeTestRule.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(originalDensity.density, 2f)) {
                val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)
                SideEffect {
                    keyboardVisible = keyboardHeight > 0
                }
                HeartGuardTheme {
                    Box(Modifier.requiredSize(393.dp, 582.dp)) {
                        AuthLoginScreen(
                            email = "",
                            password = passwordState.value,
                            onEmailChange = {},
                            onPasswordChange = { changedPassword -> passwordState.value = changedPassword },
                            onLoginClick = {},
                        )
                    }
                }
            }
        }
        val passwordField = composeTestRule.onAllNodes(hasSetTextAction())[1]
        passwordField.performScrollTo().performTouchInput { click() }
        composeTestRule.waitUntil(5_000) {
            val passwordNode = passwordField.fetchSemanticsNode()
            keyboardVisible && passwordNode.boundsInRoot.height >= passwordNode.size.height - 1f
        }
        passwordField.assertIsFocused().assertIsDisplayed().performTextInput("focus-check")
        composeTestRule.waitUntil(5_000) { passwordState.value == "focus-check" }
        composeTestRule.runOnIdle { assertEquals("focus-check", passwordState.value) }
        Espresso.pressBack()
        composeTestRule.waitUntil(5_000) { !keyboardVisible }
        passwordField.performScrollTo().performTouchInput { click() }
        composeTestRule.waitUntil(5_000) {
            val passwordNode = passwordField.fetchSemanticsNode()
            keyboardVisible && passwordNode.boundsInRoot.height >= passwordNode.size.height - 1f
        }
        passwordField.assertIsFocused().assertIsDisplayed()
    }

}
