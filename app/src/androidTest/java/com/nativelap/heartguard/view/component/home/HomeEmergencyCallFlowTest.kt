package com.nativelap.heartguard.view.component.home

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.test.espresso.Espresso
import com.nativelap.heartguard.navigation.HeartGuardBottomSheetSceneStrategy
import com.nativelap.heartguard.navigation.HeartGuardDestination
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.route.home.HomeHeadquartersCallConfirmationRoute
import com.nativelap.heartguard.view.route.home.HomeHeadquartersContactRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeEmergencyCallFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun headquartersContactOpensConfirmationAndCancelReturnsWithoutCalling() {
        var didRequestCall = false
        var dialogBlurRadius = 0
        var launchedIntent: Intent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                HeadquartersCallNavigationTestContent(
                    onDialerOpened = {
                        didRequestCall = true
                    },
                    onIntentLaunched = { launchedIntent = it },
                    onDialogBlurMeasured = {
                        dialogBlurRadius = it
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("긴급 전화 열기").performClick()
        composeTestRule.onNodeWithText("본사").assertExists()
        composeTestRule.onNodeWithText("010-5080-4623").assertExists()
        composeTestRule.onNodeWithContentDescription("전화 걸기").performClick()

        composeTestRule.onNodeWithText("본사 연락하기").assertExists()
        composeTestRule
            .onNodeWithTag("homeEmergencyCallConfirmationText")
            .assertTextEquals("본사 연락하기")
        composeTestRule.onNodeWithTag("homeEmergencyCallConfirmation").captureToImage().let { image ->
            val topCenterPixel = image.toPixelMap()[image.width / 2, 1]
            assertEquals(Color.White, topCenterPixel)
        }
        composeTestRule.runOnIdle {
            assertEquals(true, dialogBlurRadius > 0)
        }

        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.onNodeWithText("본사").assertExists()
        composeTestRule.onNodeWithText("010-5080-4623").assertExists()

        composeTestRule.runOnIdle {
            assertEquals(false, didRequestCall)
            assertEquals(null, launchedIntent)
        }
    }

    @Test
    fun confirmationShowsDestinationAndOpensSystemDialer() {
        var launchedIntent: Intent? = null
        var didOpenDialer = false
        composeTestRule.setContent {
            HeartGuardTheme {
                HeadquartersCallNavigationTestContent(
                    onDialerOpened = {
                        didOpenDialer = true
                    },
                    onIntentLaunched = { launchedIntent = it },
                )
            }
        }

        composeTestRule.onNodeWithText("긴급 전화 열기").performClick()
        composeTestRule.onNodeWithContentDescription("전화 걸기").performClick()
        composeTestRule.onNodeWithText("본사 연락하기").assertExists()
        composeTestRule
            .onNodeWithTag("homeEmergencyCallPhoneNumber")
            .assertTextEquals("010-5080-4623")
        composeTestRule.onNodeWithText("전화 앱 열기").performClick()

        composeTestRule.runOnIdle {
            assertEquals(Intent.ACTION_DIAL, launchedIntent?.action)
            assertEquals(Uri.fromParts("tel", "01050804623", null), launchedIntent?.data)
            assertEquals(true, didOpenDialer)
        }
    }

    @Test
    fun failedDialerLaunchCanBeRetried() {
        var launchAttempts = 0
        var launchedIntent: Intent? = null
        var didOpenDialer = false
        composeTestRule.setContent {
            HeartGuardTheme {
                HeadquartersCallNavigationTestContent(
                    onDialerOpened = {
                        didOpenDialer = true
                    },
                    onIntentLaunched = { intent ->
                        launchAttempts += 1
                        if (launchAttempts == 1) {
                            throw ActivityNotFoundException("No dialer activity")
                        }
                        launchedIntent = intent
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("긴급 전화 열기").performClick()
        composeTestRule.onNodeWithContentDescription("전화 걸기").performClick()
        composeTestRule.onNodeWithText("전화 앱 열기").performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, launchAttempts)
            assertEquals(null, launchedIntent)
            assertEquals(false, didOpenDialer)
        }

        composeTestRule.onNodeWithText("전화 앱 열기").performClick()

        composeTestRule.runOnIdle {
            assertEquals(2, launchAttempts)
            assertEquals(Intent.ACTION_DIAL, launchedIntent?.action)
            assertEquals(true, didOpenDialer)
        }
    }

    @Test
    fun systemBackFromConfirmationReturnsToHeadquartersContact() {
        var launchedIntent: Intent? = null
        composeTestRule.setContent {
            HeartGuardTheme {
                HeadquartersCallNavigationTestContent(
                    onDialerOpened = {},
                    onIntentLaunched = { launchedIntent = it },
                )
            }
        }

        composeTestRule.onNodeWithText("긴급 전화 열기").performClick()
        composeTestRule.onNodeWithContentDescription("전화 걸기").performClick()
        composeTestRule.onNodeWithText("본사 연락하기").assertExists()

        Espresso.pressBack()

        composeTestRule.onNodeWithText("본사").assertExists()
        composeTestRule.onNodeWithText("010-5080-4623").assertExists()
        composeTestRule.runOnIdle {
            assertEquals(null, launchedIntent)
        }
    }

    @Test
    fun headquartersConfirmationInvokesDialerRequestCallback() {
        var didRequestCall = false
        composeTestRule.setContent {
            HeartGuardTheme {
                HomeEmergencyPhoneCallDialog(
                    phoneNumber = "010-5080-4623",
                    onDismissRequest = {},
                    onCallClick = {
                        didRequestCall = true
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("전화 앱 열기").performClick()

        composeTestRule.runOnIdle {
            assertEquals(true, didRequestCall)
        }
    }
}

@Composable
private fun HeadquartersCallNavigationTestContent(
    onDialerOpened: () -> Unit,
    onIntentLaunched: (Intent) -> Unit = {},
    onDialogBlurMeasured: ((Int) -> Unit)? = null,
) {
    val baseContext = LocalContext.current
    val dialerContext =
        remember(baseContext, onIntentLaunched) {
            object : ContextWrapper(baseContext) {
                override fun startActivity(intent: Intent) {
                    onIntentLaunched(intent)
                }
            }
        }

    CompositionLocalProvider(LocalContext provides dialerContext) {
        val backStack = rememberNavBackStack(HeartGuardDestination.Home)
        val sceneStrategies: List<SceneStrategy<androidx.navigation3.runtime.NavKey>> =
            listOf(
                DialogSceneStrategy(),
                HeartGuardBottomSheetSceneStrategy(),
                SinglePaneSceneStrategy(),
            )

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            sceneStrategies = sceneStrategies,
            entryProvider =
                entryProvider {
                    entry<HeartGuardDestination.Home> {
                        Column {
                            Text("홈")
                            Button(
                                onClick = {
                                    backStack.add(HeartGuardDestination.HeadquartersContact)
                                },
                            ) {
                                Text("긴급 전화 열기")
                            }
                        }
                    }
                    entry<HeartGuardDestination.HeadquartersContact>(
                        metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(),
                    ) {
                        HomeHeadquartersContactRoute(
                            onCallClick = {
                                backStack.add(HeartGuardDestination.HeadquartersCallConfirmation)
                            },
                        )
                    }
                    entry<HeartGuardDestination.HeadquartersCallConfirmation>(
                        metadata =
                            DialogSceneStrategy.dialog(
                                DialogProperties(usePlatformDefaultWidth = false),
                            ),
                    ) {
                        CompositionLocalProvider(LocalContext provides dialerContext) {
                            HomeHeadquartersCallConfirmationRoute(
                                onDismissRequest = {
                                    backStack.removeLastOrNull()
                                },
                                onDialerOpened = onDialerOpened,
                            )
                            if (onDialogBlurMeasured != null) {
                                DialogBlurRadiusProbe(onBlurRadiusMeasured = onDialogBlurMeasured)
                            }
                        }
                    }
                },
        )
    }
}

@Composable
private fun DialogBlurRadiusProbe(onBlurRadiusMeasured: (Int) -> Unit) {
    val dialogWindow =
        (LocalView.current.parent as? DialogWindowProvider)
            ?.window

    SideEffect {
        onBlurRadiusMeasured(dialogWindow?.attributes?.blurBehindRadius ?: 0)
    }
}
