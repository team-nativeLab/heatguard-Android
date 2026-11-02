package com.nativelap.heartguard.view.component.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePhotoSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun optionsAndCancelInvokeTheirOwnCallbacks() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            HeartGuardTheme {
                ProfilePhotoSheet(
                    onDismissRequest = { calls += "dismiss" },
                    onCaptureClick = { calls += "capture" },
                    onAlbumClick = { calls += "album" },
                    onDefaultPhotoClick = { calls += "default" },
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.profile_photo_sheet_title))
            .assertExists()
        composeRule
            .onNodeWithText(context.getString(R.string.profile_photo_sheet_capture))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.profile_photo_sheet_album))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.profile_photo_sheet_default))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(R.string.common_cancel))
            .performClick()

        assertEquals(listOf("capture", "album", "default", "dismiss"), calls)
    }
}
