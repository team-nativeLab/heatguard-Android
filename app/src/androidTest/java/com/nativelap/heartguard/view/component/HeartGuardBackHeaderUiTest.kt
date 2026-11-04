package com.nativelap.heartguard.view.component

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class HeartGuardBackHeaderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backButtonClickCallsBackClickOnce() {
        var backClickCount = 0
        var notificationClickCount = 0
        setHeader(
            onBackClick = { backClickCount++ },
            onNotificationClick = { notificationClickCount++ },
        )

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.common_back_description))
            .performClick()

        assertEquals(1, backClickCount)
        assertEquals(0, notificationClickCount)
    }

    @Test
    fun notificationButtonClickCallsNotificationClick() {
        var backClickCount = 0
        var notificationClickCount = 0
        setHeader(
            onBackClick = { backClickCount++ },
            onNotificationClick = { notificationClickCount++ },
        )

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.common_notifications))
            .performClick()

        assertEquals(0, backClickCount)
        assertEquals(1, notificationClickCount)
    }

    @Test
    fun unreadNotificationsChangeNotificationButtonDescription() {
        setHeader(
            onBackClick = {},
            onNotificationClick = {},
            hasUnreadNotifications = true,
        )

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.common_notifications_unread))
            .assertIsDisplayed()
    }

    private fun setHeader(
        onBackClick: () -> Unit,
        onNotificationClick: () -> Unit,
        hasUnreadNotifications: Boolean = false,
    ) {
        composeRule.setContent {
            HeartGuardTheme {
                HeartGuardBackHeader(
                    title = context.getString(R.string.brand_name),
                    notificationPainter = painterResource(R.drawable.notification_bell),
                    onBackClick = onBackClick,
                    onNotificationClick = onNotificationClick,
                    hasUnreadNotifications = hasUnreadNotifications,
                )
            }
        }
    }
}
