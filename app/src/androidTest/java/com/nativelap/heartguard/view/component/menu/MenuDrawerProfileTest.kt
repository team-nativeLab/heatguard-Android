package com.nativelap.heartguard.view.component.menu

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.viewmodel.menu.MenuDrawerProfileUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@OptIn(DelicateCoilApi::class)
class MenuDrawerProfileTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loaded https photo exposes drawer image description`() {
        val fetcherFactory = PhotoFetcherFactory(shouldFail = false)
        val previousImageLoader = replaceSingletonImageLoader(fetcherFactory)

        try {
            composeRule.setContent {
                HeartGuardTheme {
                    MenuDrawerProfile(
                        profile =
                            MenuDrawerProfileUiModel(
                                userName = "현장 작업자",
                                photoUrl = "https://images.example.test/profile",
                            ),
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) {
                fetcherFactory.finishedRequestCount.get() > 0
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithContentDescription("사용자 프로필 사진").assertExists()
            composeRule.onNodeWithText("현", substring = false).assertDoesNotExist()

            assertTrue(fetcherFactory.startedRequestCount.get() > 0)
        } finally {
            composeRule.waitForIdle()
            SingletonImageLoader.setUnsafe(previousImageLoader)
        }
    }

    @Test
    fun `failed https photo request falls back to the name initial`() {
        val fetcherFactory = PhotoFetcherFactory(shouldFail = true)
        val previousImageLoader = replaceSingletonImageLoader(fetcherFactory)

        try {
            composeRule.setContent {
                HeartGuardTheme {
                    MenuDrawerProfile(
                        profile =
                            MenuDrawerProfileUiModel(
                                userName = "현장 작업자",
                                photoUrl = "https://images.example.test/profile",
                            ),
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = REQUEST_TIMEOUT_MILLIS) {
                fetcherFactory.finishedRequestCount.get() > 0
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithText("현", substring = false).assertIsDisplayed()
            assertTrue(fetcherFactory.startedRequestCount.get() > 0)
        } finally {
            composeRule.waitForIdle()
            SingletonImageLoader.setUnsafe(previousImageLoader)
        }
    }

    @Test
    fun `missing and non https photo urls show the initial without starting image requests`() {
        val fetcherFactory = PhotoFetcherFactory(shouldFail = true)
        val previousImageLoader = replaceSingletonImageLoader(fetcherFactory)
        val profile = mutableStateOf(MenuDrawerProfileUiModel(userName = "현장 작업자"))

        try {
            composeRule.setContent {
                HeartGuardTheme {
                    MenuDrawerProfile(profile = profile.value)
                }
            }
            composeRule.onNodeWithText("현", substring = false).assertIsDisplayed()
            composeRule.onNodeWithContentDescription("사용자 프로필 사진").assertDoesNotExist()

            composeRule.runOnIdle {
                profile.value =
                    MenuDrawerProfileUiModel(
                        userName = "현장 작업자",
                        photoUrl = "http://images.example.test/profile",
                    )
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("현", substring = false).assertIsDisplayed()
            composeRule.onNodeWithContentDescription("사용자 프로필 사진").assertDoesNotExist()
            composeRule.waitForIdle()

            assertEquals(0, fetcherFactory.startedRequestCount.get())
        } finally {
            composeRule.waitForIdle()
            SingletonImageLoader.setUnsafe(previousImageLoader)
        }
    }

    private fun replaceSingletonImageLoader(fetcherFactory: PhotoFetcherFactory): ImageLoader {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousImageLoader = SingletonImageLoader.get(context)
        SingletonImageLoader.setUnsafe(
            ImageLoader
                .Builder(context)
                .components {
                    add(fetcherFactory)
                }.build(),
        )
        return previousImageLoader
    }

    private class PhotoFetcherFactory(
        private val shouldFail: Boolean,
    ) : Fetcher.Factory<Any> {
        val startedRequestCount = AtomicInteger(0)
        val finishedRequestCount = AtomicInteger(0)

        override fun create(
            data: Any,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher =
            Fetcher {
                startedRequestCount.incrementAndGet()
                try {
                    if (shouldFail) {
                        throw IOException("Synthetic image request failure")
                    }
                    ImageFetchResult(
                        image = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImage(),
                        isSampled = false,
                        dataSource = DataSource.MEMORY,
                    )
                } finally {
                    finishedRequestCount.incrementAndGet()
                }
            }
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 5_000L
    }
}
