package com.nativelap.heartguard.view.screen.site

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Rule
import org.junit.Test

class TeamSiteSelectionResponsiveTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectedSiteAndAddLinkRemainReachableOnShortScreen() {
        composeTestRule.setContent {
            HeartGuardTheme {
                Box(Modifier.requiredSize(360.dp, 360.dp)) {
                    TeamSiteSelectionScreen(
                        siteName = "현재 작업 현장",
                        isLoading = false,
                        hasLoadError = false,
                        onBackClick = {},
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("현재 작업 현장")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("새로운 현장 연동하기").assertIsDisplayed()
    }

    @Test
    fun selectedSiteAndAddLinkRemainReachableWithExpandedFont() {
        composeTestRule.setContent {
            val originalDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(originalDensity.density, 2f),
            ) {
                HeartGuardTheme {
                    Box(Modifier.requiredSize(360.dp, 480.dp)) {
                        TeamSiteSelectionScreen(
                            siteName = "현재 작업 현장",
                            isLoading = false,
                            hasLoadError = false,
                            onBackClick = {},
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithText("현재 작업 현장")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("새로운 현장 연동하기").assertIsDisplayed()
    }
}
